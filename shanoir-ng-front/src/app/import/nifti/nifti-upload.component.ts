/**
 * Shanoir NG - Import, manage and share neuroimaging data
 * Copyright (C) 2009-2019 Inria - https://www.inria.fr/
 * Contact us on https://project.inria.fr/shanoir/
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.html
 */

import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { BreadcrumbsService } from '../../breadcrumbs/breadcrumbs.service';
import { slideDown } from '../../shared/animations/animations';
import { ImportDataService } from '../shared/import.data-service';
import { ImportService } from '../shared/import.service';
import { ImportJob } from '../shared/dicom-data.model';
import { Subject } from '../../subjects/shared/subject.model';
import { Study } from '../../studies/shared/study.model';
import { IdName } from '../../shared/models/id-name.model';
import { Dataset } from '../../datasets/shared/dataset.model'
import { DatasetAcquisition } from '../../dataset-acquisitions/shared/dataset-acquisition.model';
import { ExaminationService } from '../../examinations/shared/examination.service'
import { StudyService } from '../../studies/shared/study.service';
import { DatasetService } from '../../datasets/shared/dataset.service';
import { SubjectWithSubjectStudy } from '../../subjects/shared/subject.with.subject-study.model';


type Status = 'none' | 'uploading' | 'uploaded' | 'error';

@Component({
    selector: 'nifti-upload',
    templateUrl: 'nifti-upload.component.html',
    styleUrls: ['nifti-upload.component.css', '../shared/import.step.css'],
    animations: [slideDown]
})

export class NiftiUploadComponent {
    
    private modality: string;
    
    protected archiveStatus: Status = 'none';
    protected extensionError: boolean;
    protected errorMessage: string;

    protected study: Study;
    protected subject: Subject;
    protected parentDataset: Dataset;

    protected studies: Study[] = [];
    protected subjects: SubjectWithSubjectStudy[] = [];
    protected datasets: Dataset[] = [];

    private importJob: ImportJob;

    constructor(
            private importService: ImportService,
            private studyService: StudyService,
            private datasetService: DatasetService,
            private router: Router,
            private breadcrumbsService: BreadcrumbsService,
            private examinationService: ExaminationService,
            private importDataService: ImportDataService) {
        
        breadcrumbsService.nameStep('1. Upload');
        breadcrumbsService.markMilestone();

        // Initialize studies;
        this.studyService.getStudyNamesAndCenters().then((allStudies) => {
            this.studies = allStudies;
        });
    }

    private uploadArchive(fileEvent: any): void {
        this.setArchiveStatus('uploading');
        this.uploadToServer(fileEvent.target.files);
    }

    private uploadToServer(file: any) {
        this.extensionError = file[0].name.substring(file[0].name.lastIndexOf("."), file[0].name.length) != '.zip';

        this.modality = null;
        let formData: FormData = new FormData();
        formData.append('file', file[0], file[0].name);
        this.importService.uploadNiftiFile(formData)
            .then((importJob: ImportJob) => {
                this.setArchiveStatus('uploaded');
                this.errorMessage = "";
                this.importJob = importJob;
            }).catch(error => {
                this.setArchiveStatus('error');
                if (error && error.error && error.error.message) {
                        this.errorMessage = error.error.message;
                    }
            });
    }

    private onSelectStudy(): void {
        // Get all subjects for study
        this.subjects = [];
        this.studyService
                .findSubjectsByStudyId(this.study.id)
                .then(subjects => this.subjects = subjects);
    }

    private onSelectSubject(): void {
        // Get all datasets for subject in study
        let datasetsToSet = [];
        this.examinationService
            .findExaminationsBySubjectAndStudy(this.subject.id, this.study.id)
            .then(exams => {
                if (!exams || exams.length == 0) {
                    // do nothing
                    return;
                }
                for (let exami of exams) {
                    for (let acq of exami.datasetAcquisitions) {
                        for (let ds of acq.datasets) {
                            datasetsToSet.push(ds);
                        }
                    }
                }
                this.datasets = datasetsToSet;
            });
    }

    private setArchiveStatus(status: Status) {
        this.archiveStatus = status;
    }

    get valid(): boolean {
        return (this.archiveStatus == 'uploaded' && this.parentDataset != null);
    }

    private next() {
        this.importJob.parentDatasetId = this.parentDataset.id;
        this.importJob.studyId = this.study.id;
        this.importService.startNiftiImportJob(this.importJob);
        this.router.navigate(['dataset/list']);
    }

}