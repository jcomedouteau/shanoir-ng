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
import { Component, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { AcquisitionEquipment } from '../../acquisition-equipments/shared/acquisition-equipment.model';
import { AcquisitionEquipmentPipe } from '../../acquisition-equipments/shared/acquisition-equipment.pipe';
import { BreadcrumbsService, Step } from '../../breadcrumbs/breadcrumbs.service';
import { Center } from '../../centers/shared/center.model';
import { CenterService } from '../../centers/shared/center.service';
import { Coil } from '../../coils/shared/coil.model';
import { Examination } from '../../examinations/shared/examination.model';
import { ExaminationService } from '../../examinations/shared/examination.service';
import { SubjectExamination } from '../../examinations/shared/subject-examination.model';
import { SubjectExaminationPipe } from '../../examinations/shared/subject-examination.pipe';
import { NiftiConverter } from '../../niftiConverters/nifti.converter.model';
import { NiftiConverterService } from '../../niftiConverters/nifti.converter.service';
import { AnimalSubject } from '../../preclinical/animalSubject/shared/animalSubject.model';
import { AnimalSubjectService } from '../../preclinical/animalSubject/shared/animalSubject.service';
import { PreclinicalSubject } from '../../preclinical/animalSubject/shared/preclinicalSubject.model';
import { preventInitialChildAnimations, slideDown } from '../../shared/animations/animations';
import { KeycloakService } from '../../shared/keycloak/keycloak.service';
import { IdName } from '../../shared/models/id-name.model';
import { Option } from '../../shared/select/select.component';
import { StudyCenter } from '../../studies/shared/study-center.model';
import { StudyRightsService } from '../../studies/shared/study-rights.service';
import { StudyUserRight } from '../../studies/shared/study-user-right.enum';
import { Study } from '../../studies/shared/study.model';
import { StudyService } from '../../studies/shared/study.service';
import { StudyCard } from '../../study-cards/shared/study-card.model';
import { StudyCardService } from '../../study-cards/shared/study-card.service';
import { ImagedObjectCategory } from '../../subjects/shared/imaged-object-category.enum';
import { SubjectStudy } from '../../subjects/shared/subject-study.model';
import { Subject } from '../../subjects/shared/subject.model';
import { SubjectService } from '../../subjects/shared/subject.service';
import { SubjectWithSubjectStudy } from '../../subjects/shared/subject.with.subject-study.model';
import { EquipmentDicom, PatientDicom } from '../shared/dicom-data.model';
import { ContextData, ImportDataService } from '../shared/import.data-service';
import { DatasetModalityType } from '../../enum/dataset-modality-type.enum';



@Component({
    selector: 'clinical-context',
    templateUrl: 'clinical-context.component.html',
    styleUrls: ['clinical-context.component.css', '../shared/import.step.css'],
    animations: [slideDown, preventInitialChildAnimations]
})
export class ClinicalContextComponent implements OnDestroy {
    
    patient: PatientDicom;
    private studyOptions: Option<Study>[] = [];
    private studycardOptions: Option<StudyCard>[] = [];
    private centerOptions: Option<Center>[] = [];
    private allCenters: Center[];
    private acquisitionEquipmentOptions: Option<AcquisitionEquipment>[] = [];
    private subjects: SubjectWithSubjectStudy[] = [];
    private examinations: SubjectExamination[] = [];
    private niftiConverters: NiftiConverter[] = [];
    private study: Study;
    private studycard: StudyCard;
    private center: Center;
    private acquisitionEquipment: AcquisitionEquipment;
    private subject: SubjectWithSubjectStudy;
    private examination: SubjectExamination;
    private niftiConverter: NiftiConverter;
    private animalSubject: AnimalSubject = new AnimalSubject();
    private importMode: 'DICOM' | 'PACS' | 'EEG' | 'BRUKER' | 'BIDS';
    private subscribtions: Subscription[] = [];
    public subjectTypes: Option<string>[] = [
        new Option<string>('HEALTHY_VOLUNTEER', 'Healthy Volunteer'),
        new Option<string>('PATIENT', 'Patient'),
        new Option<string>('PHANTOM', 'Phantom')
    ];
    public useStudyCard: boolean = true;
    protected scHasCoilToUpdate: boolean;
    protected isAdminOfStudy: boolean[] = [];
    protected scHasDifferentModality: string;
    private modality: string;
    
    constructor(
            private studyService: StudyService,
            private centerService: CenterService,
            private niftiConverterService: NiftiConverterService,
            private subjectService: SubjectService,
            private examinationService: ExaminationService,
            private animalSubjectService: AnimalSubjectService,
            private router: Router,
            private breadcrumbsService: BreadcrumbsService,
            private importDataService: ImportDataService,
            public subjectExaminationLabelPipe: SubjectExaminationPipe,
            private acqEqPipe: AcquisitionEquipmentPipe,
            private studycardService: StudyCardService,
            private studyRightsService: StudyRightsService,
            private keycloakService: KeycloakService) {

        if (!importDataService.patients || !importDataService.patients[0]) {
            this.router.navigate(['imports'], {replaceUrl: true});
            return;
        }
        breadcrumbsService.nameStep('3. Context'); 

        this.importMode = this.breadcrumbsService.findImportMode();
        
        this.setPatient(this.importDataService.patients[0]).then(() => {
            this.reloadSavedData();
            this.onContextChange();
        });

        this.niftiConverters = [];
        this.niftiConverterService.getAll().then(niftiConverters => this.niftiConverters = niftiConverters);
    }

    private reloadSavedData() {
        if (this.importDataService.contextBackup) {
            let study = this.importDataService.contextBackup.study;
            let studyCard = this.importDataService.contextBackup.studyCard;
            let useStudyCard = this.importDataService.contextBackup.useStudyCard;
            let center = this.importDataService.contextBackup.center;
            let acquisitionEquipment = this.importDataService.contextBackup.acquisitionEquipment;
            let subject = this.importDataService.contextBackup.subject;
            let examination = this.importDataService.contextBackup.examination;
            let niftiConverter = this.importDataService.contextBackup.niftiConverter;
            if (study) {
                this.study = study;
                let studyOption = this.studyOptions.find(s => s.value.id == study.id);
                if (studyOption) {
                    this.study = studyOption.value; // in case it has been modified by an on-the-fly equipment creation
                }
                this.onSelectStudy().then(() => {
                    if (this.useStudyCard != useStudyCard) { 
                        this.useStudyCard = useStudyCard;
                        this.onToggleUseStudyCard();
                    } else if (useStudyCard && studyCard){
                        this.studycard = studyCard;
                        this.onSelectStudyCard();
                    }
                    if (center) {
                        this.center = center;
                        let centerOption = this.centerOptions.find(c => c.value.id == center.id);
                        if (centerOption) {
                            this.center = centerOption.value;  // in case it has been modified by an on-the-fly equipment creation
                        }
                        this.onSelectCenter();
                    }
                    if (acquisitionEquipment) {
                        this.acquisitionEquipment = acquisitionEquipment;
                        this.onSelectAcquisitonEquipment();
                    }
                    if (subject) {
                        this.subject = subject;
                        this.onSelectSubject();
                    }
                    if (examination) {
                        this.examination = examination;
                        this.onSelectExam();
                    }
                    if (niftiConverter) {
                        this.niftiConverter = niftiConverter;
                    }
                });
            }
        }
    }

    setPatient(patient: PatientDicom): Promise<void> {
        this.patient = patient;
        this.modality = this.patient.studies[0].series[0].modality.toString();
        this.useStudyCard = this.modality.toUpperCase() != 'CT';
        return this.completeStudiesCompatibilities(this.patient.studies[0].series[0].equipment)
            /* For the moment, we import only zip files with the same equipment, 
            That's why the calculation is only based on the equipment of the first series of the first study */
            .then(() => {
                let compatibleFounded = this.studyOptions.find(study => study.compatible);
                if (compatibleFounded) {
                    this.study = compatibleFounded.value;
                    this.onSelectStudy();
                }
            })
    }

    private completeStudiesCompatibilities(equipment: EquipmentDicom): Promise<void> {
        return Promise.all([this.studyService.getStudyNamesAndCenters(), this.centerService.getAll()])
            .then(([allStudies, allCenters]) => {
                this.studyOptions = [];
                this.allCenters = allCenters;
                for (let study of allStudies) {
                    let studyOption: Option<Study> = new Option(study, study.name);
                    if (this.importMode == 'DICOM') studyOption.compatible = false;
                    if (study.studyCenterList) {
                        for (let studyCenter of study.studyCenterList) {
                            let center: Center = allCenters.find(center => center.id === studyCenter.center.id);
                            if (center) {
                                if (this.importMode == 'DICOM' && this.centerCompatible(center)) {
                                    studyOption.compatible = true;
                                }
                                studyCenter.center = center;
                            } 
                        }
                        this.studyOptions.push(studyOption);
                        // update the selected study as well
                        if (this.study && this.study.id == study.id) {
                            this.study.studyCenterList = study.studyCenterList; 
                        }
                    }
                }
            });
    }

    private equipmentsEquals(eq1: AcquisitionEquipment, eq2: EquipmentDicom): boolean {
        return eq1.serialNumber === eq2.deviceSerialNumber
        && eq1.manufacturerModel.name === eq2.manufacturerModelName
        && eq1.manufacturerModel.manufacturer.name === eq2.manufacturer;
    }

    public acqEqCompatible(acquisitionEquipment: AcquisitionEquipment): boolean {
        return this.equipmentsEquals(acquisitionEquipment, this.patient.studies[0].series[0].equipment);
    }
    
    public centerCompatible(center: Center): boolean {
        return center.acquisitionEquipments && center.acquisitionEquipments.find(eq => this.acqEqCompatible(eq)) != undefined;
    }

    private onSelectStudy(): Promise<void> {
        let end: Promise<void> = Promise.resolve();
        if (this.useStudyCard) {
            this.studycard = this.center = this.acquisitionEquipment = this.subject = this.examination = null;
            if (this.study) {
                let studyEquipments: AcquisitionEquipment[] = [];
                this.study.studyCenterList.forEach(sc => {
                    sc.center.acquisitionEquipments.forEach(eq => {
                        if (studyEquipments.findIndex(se => se.id == eq.id) == -1) studyEquipments.push(eq);
                    });
                });
                end = this.studycardService.getAllForStudy(this.study.id).then(studycards => {
                    if (!studycards) studycards = [];
                    this.studycardOptions = studycards.map(sc => {
                        let opt = new Option(sc, sc.name);
                        if (sc.acquisitionEquipment) {
                            let scEq = studyEquipments.find(se => se.id == sc.acquisitionEquipment.id);
                            if (this.importMode == 'DICOM') opt.compatible = this.acqEqCompatible(scEq);
                            if (!this.studycard && opt.compatible) {
                                this.studycard = sc;
                                this.onSelectStudyCard();
                            }
                        } else if (this.importMode == 'DICOM') opt.compatible = false;
                        return opt;
                    });
                    //if (!this.studycard) this.useStudyCard = false;
                });
            }
        }
        this.centerOptions = []; 
        this.acquisitionEquipmentOptions = []; 
        this.subjects = [];
        this.examinations = [];
        let foundCompatibleCenter: boolean = false;
        if (this.study && this.study.id && this.study.studyCenterList) {
            for (let studyCenter of this.study.studyCenterList) {
                let centerOption = new Option<Center>(studyCenter.center, studyCenter.center.name);
                if (!this.useStudyCard && this.importMode == 'DICOM') {
                    centerOption.compatible = studyCenter.center && this.centerCompatible(studyCenter.center);
                    if (!foundCompatibleCenter && centerOption.compatible) {
                        foundCompatibleCenter = true;
                        this.center = centerOption.value;
                        this.onSelectCenter();
                    }
                }
                this.centerOptions.push(centerOption);
            }
        }
        return end.then(() => this.onContextChange());
    }

    private onSelectStudyCard(): void {
        if (this.study && this.studycard && this.studycard.acquisitionEquipment) {
            this.acquisitionEquipment = null;
            let scFound = this.study.studyCenterList.find(sc => {
                let eqFound = sc.center.acquisitionEquipments.find(eq => eq.id == this.studycard.acquisitionEquipment.id);
                if (eqFound) return true;
                else return false;
            })
            this.center = scFound ? scFound.center : null;
            this.onSelectCenter();
            this.acquisitionEquipment = this.studycard.acquisitionEquipment;
            this.onSelectAcquisitonEquipment();
            this.niftiConverter = this.studycard.niftiConverter;
            
        }
        this.scHasCoilToUpdate = this.hasCoilToUpdate(this.studycard);
        this.scHasDifferentModality = this.hasDifferentModality(this.studycard);
        if (this.isAdminOfStudy[this.study.id] == undefined) {
            this.hasAdminRightOn(this.study).then((result) => this.isAdminOfStudy[this.study.id] = result);
        }
        this.onContextChange();
    }

    onToggleUseStudyCard() {
        if (!this.useStudyCard) this.studycard = null;
        else {
            let studycardOpt = this.studycardOptions.find(sco => sco.compatible == true);
            if (studycardOpt) {
                this.studycard = studycardOpt.value;
                this.onSelectStudyCard();
            }
        }
        this.importDataService.contextBackup.useStudyCard = this.useStudyCard;;
        
    }

    private onSelectCenter(): void {
        this.acquisitionEquipment = this.subject = this.examination = null;
        this.acquisitionEquipmentOptions =  [];
        this.subjects =  [];
        this.examinations = [];
        if (this.center && this.center.acquisitionEquipments) {
            for (let acqEq of this.center.acquisitionEquipments) {
                let option = new Option<AcquisitionEquipment>(acqEq, this.acqEqPipe.transform(acqEq));
                if (this.importMode == 'DICOM') {
                    option.compatible = this.acqEqCompatible(acqEq);
                    if (option.compatible) {
                        this.acquisitionEquipment = option.value;
                        this.onSelectAcquisitonEquipment();
                    }
                }
                this.acquisitionEquipmentOptions.push(option);
            }
        }
        this.onContextChange();
    }

    private onSelectAcquisitonEquipment(): void {
        this.subject = this.examination = null;
        this.subjects =  [];
        this.examinations = [];
        if (this.acquisitionEquipment) {
            if(this.importMode == 'BRUKER') {
                this.studyService
                .findSubjectsByStudyIdPreclinical(this.study.id, true)
                .then(subjects => this.subjects = subjects);
            } else {
                this.studyService
                    .findSubjectsByStudyId(this.study.id)
                    .then(subjects => this.subjects = subjects);
            }
        }
        this.onContextChange();
    }

    private onSelectSubject(): void {
        this.examination = null;
        this.examinations = [];
        if (this.subject) {
            if(this.importMode == 'BRUKER') {
            this.animalSubjectService
        		.findAnimalSubjectBySubjectId(this.subject.id)
        		.then(animalSubject => this.animalSubject = animalSubject);
            }
            this.examinationService
                .findExaminationsBySubjectAndStudy(this.subject.id, this.study.id)
                .then(examinations => this.examinations = examinations);
        }
        this.onContextChange();
    }

    private onSelectExam(): void {
        this.onContextChange();
    }

    private onSelectNifti(): void {
        this.onContextChange();
    }

    private onContextChange() {
        this.importDataService.contextBackup = this.getContext();
        if (this.valid) {
            this.importDataService.contextData = this.getContext();
        }
    }
    
    private getContext(): ContextData {
        return new ContextData(this.study, this.studycard, this.useStudyCard, this.center, this.acquisitionEquipment,
            this.subject, this.examination, this.niftiConverter, null);
    }

    private openCreateCenter = () => {
        let currentStep: Step = this.breadcrumbsService.currentStep;
        this.router.navigate(['/center/create']).then(success => {
            this.breadcrumbsService.currentStep.entity = this.getPrefilledCenter();
            this.subscribtions.push(
                currentStep.waitFor(this.breadcrumbsService.currentStep, false).subscribe(entity => {
                    this.importDataService.contextBackup.center = this.updateStudyCenter(entity as Center);
                })
            );
        });
    }

    private getPrefilledCenter(): Center {
        let studyCenter = new StudyCenter();
        studyCenter.study = this.study;
        let newCenter = new Center();
        newCenter.studyCenterList = [studyCenter];
        return newCenter;
    }

    private updateStudyCenter(center: Center): Center {
        if (!center) return;
        let studyCenter: StudyCenter = center.studyCenterList[0];
        if (studyCenter) this.study.studyCenterList.push(studyCenter);
        return center;
    }

    private openCreateAcqEqt() {
        let currentStep: Step = this.breadcrumbsService.currentStep;
        this.router.navigate(['/acquisition-equipment/create']).then(success => {
            this.breadcrumbsService.currentStep.entity = this.getPrefilledAcqEqt();
            this.subscribtions.push(
                currentStep.waitFor(this.breadcrumbsService.currentStep, false).subscribe(entity => {
                    this.importDataService.contextBackup.acquisitionEquipment = (entity as AcquisitionEquipment);
                })
            );
        });
    }

    private getPrefilledAcqEqt(): AcquisitionEquipment {
        let acqEpt = new AcquisitionEquipment();
        acqEpt.center = this.center;
        acqEpt.serialNumber = this.patient.studies[0].series[0].equipment.deviceSerialNumber;
        return acqEpt;
    }

    private openCreateSubject = () => {
        let importStep: Step = this.breadcrumbsService.currentStep;
        let createSubjectRoute: string = this.importMode == 'BRUKER' ? '/preclinical-subject/create' : '/subject/create';
        this.router.navigate([createSubjectRoute]).then(success => {
            this.breadcrumbsService.currentStep.entity = this.getPrefilledSubject();
            this.breadcrumbsService.currentStep.data.firstName = this.computeNameFromDicomTag(this.patient.patientName)[1];
            this.breadcrumbsService.currentStep.data.lastName = this.computeNameFromDicomTag(this.patient.patientName)[2];
            this.subscribtions.push(
                importStep.waitFor(this.breadcrumbsService.currentStep, false).subscribe(entity => {
                    if (this.importMode == 'BRUKER') {
                        this.importDataService.contextBackup.subject = this.subjectToSubjectWithSubjectStudy((entity as Subject));
                    } else {
                        this.importDataService.contextBackup.subject = this.subjectToSubjectWithSubjectStudy(entity as Subject);
                    }
                })
            );
        });
    }

    private getPrefilledSubject(): Subject | PreclinicalSubject {
        let subjectStudy = new SubjectStudy();
        subjectStudy.study = this.study;
        subjectStudy.physicallyInvolved = false;
        let newSubject = new Subject();
        newSubject.birthDate = this.patient.patientBirthDate;
        if (this.patient.patientSex){
            newSubject.sex = this.patient.patientSex; 
        }
        newSubject.subjectStudyList = [subjectStudy];
        if (this.importMode != 'BRUKER') {
            newSubject.imagedObjectCategory = ImagedObjectCategory.LIVING_HUMAN_BEING;
            return newSubject;
        } else {
            let newPreclinicalSubject = new PreclinicalSubject();
            let newAnimalSubject = new AnimalSubject();
            newSubject.imagedObjectCategory = ImagedObjectCategory.LIVING_ANIMAL;
            newSubject.name = this.patient.patientName;
            newPreclinicalSubject.subject = newSubject;
            newPreclinicalSubject.animalSubject = newAnimalSubject;
            return newPreclinicalSubject;
        }
    }

    /**
     * Try to compute patient first name and last name from dicom tags. 
     * eg. TOM^HANKS -> return TOM as first name and HANKS as last name
     */
    private computeNameFromDicomTag (patientName: string): string[] {
        let names: string[] = [];
        if (patientName) {
            names = patientName.split("\\^");
            if (names === null || names.length !== 2) {
                names.push(patientName);
                names.push(patientName);
            }
        }
        return names;
    }
    
    private subjectToSubjectWithSubjectStudy(subject: Subject): SubjectWithSubjectStudy {
        if (!subject) return;
        let subjectWithSubjectStudy = new SubjectWithSubjectStudy();
        subjectWithSubjectStudy.id = subject.id;
        subjectWithSubjectStudy.name = subject.name;
        subjectWithSubjectStudy.identifier = subject.identifier;
        subjectWithSubjectStudy.subjectStudy = subject.subjectStudyList[0];
        return subjectWithSubjectStudy;
    }

    private openCreateExam = () => {
        let currentStep: Step = this.breadcrumbsService.currentStep;
        let createExamRoute: string = this.importMode == 'BRUKER' ? '/preclinical-examination/create' : '/examination/create';
        this.router.navigate([createExamRoute]).then(success => {
            this.breadcrumbsService.currentStep.entity = this.getPrefilledExam();
            this.subscribtions.push(
                currentStep.waitFor(this.breadcrumbsService.currentStep, false).subscribe(entity => {
                    this.importDataService.contextBackup.examination = this.examToSubjectExam(entity as Examination);
                })
            );
        });
    }

    private getPrefilledExam(): Examination {
        let newExam = new Examination();
        if (this.importMode == 'BRUKER') {
            newExam.preclinical = true;
            newExam.hasStudyCenterData = true;
        }
        newExam.study = new IdName(this.study.id, this.study.name);
        newExam.center = new IdName(this.center.id, this.center.name);
        newExam.subjectStudy = this.subject;
        newExam.subject = new Subject();
        newExam.subject.id = this.subject.id;
        newExam.subject.name = this.subject.name;
        newExam.examinationDate = this.patient.studies[0].series[0].seriesDate;
        newExam.comment = this.patient.studies[0].studyDescription;
        return newExam;
    }
    
    private examToSubjectExam(examination: Examination): SubjectExamination {
        if (!examination) return;
        // Add the new created exam to the select box and select it
        let subjectExam = new SubjectExamination();
        subjectExam.id = examination.id;
        subjectExam.examinationDate = examination.examinationDate;
        subjectExam.comment = examination.comment;
        return subjectExam;
    }

    private get hasCompatibleCenters(): boolean {
        return this.centerOptions.find(center => center.compatible) != undefined;
    }

    
    private get hasCompatibleEquipments(): boolean {
        return this.acquisitionEquipmentOptions.find(ae => ae.compatible) != undefined;
    }

    private showStudyDetails() {
        window.open('study/details/' + this.study.id, '_blank');
    }

    private showStudyCardDetails() {
        window.open('study-card/details/' + this.studycard.id, '_blank');
    }

    private showCenterDetails() {
        window.open('center/details/' + this.center.id, '_blank');
    }

    private showAcquistionEquipmentDetails() {
        window.open('acquisition-equipment/details/' + this.acquisitionEquipment.id, '_blank');
    }

    private showSubjectDetails() {
        if (this.animalSubject.id){
        	window.open('preclinical-subject/details/' + this.animalSubject.id , '_blank');
        }else{
            window.open('subject/details/' + this.subject.id, '_blank');
        }
    }

    private showExaminationDetails() {
        if (this.importMode == 'BRUKER') {
            window.open('preclinical-examination/details/' + this.examination.id , '_blank');
        } else {
            window.open('examination/details/' + this.examination.id, '_blank');
        }
    }

    get valid(): boolean {
        let context = this.getContext();
        return (
            context.study != undefined && context.study != null
            && (!context.useStudyCard || context.studyCard)
            && context.center != undefined && context.center != null
            && context.acquisitionEquipment != undefined && context.acquisitionEquipment != null
            && context.subject != undefined && context.subject != null
            && context.examination != undefined && context.examination != null
            && context.niftiConverter != undefined && context.niftiConverter != null
        );
    }

    private next() {
        if (this.importMode != 'BRUKER') {
            this.router.navigate(['imports/finish']);
        } else {
            this.router.navigate(['imports/brukerfinish']);
        }
    }

    private hasCoilToUpdate(studycard: StudyCard): boolean {
        if (!studycard) return false;
        for (let rule of studycard.rules) {
            for (let ass of rule.assignments) {
                if (ass.field.endsWith('_COIL') && !(ass.value instanceof Coil)) {
                    return true;
                }
            }
        }
        return false;
    }

    private hasDifferentModality(studycard: StudyCard): any {
        if (!studycard) return false;
        for (let rule of studycard.rules) {
            for (let ass of rule.assignments) {
                if (ass.field == 'MODALITY_TYPE' 
                        && this.modality && typeof ass.value == 'string' && ass.value
                        && (ass.value as string).split('_')[0] != this.modality.toUpperCase()) {
                    return (ass.value as string).split('_')[0];
                }
            }
        }
        return false;
    }

    protected hasAdminRightOn(study: Study): Promise<boolean> {
        if (!study) return Promise.resolve(false);
        else if (this.keycloakService.isUserAdmin()) return Promise.resolve(true);
        else if (!this.keycloakService.isUserExpert()) return Promise.resolve(false);
        else return this.studyRightsService.getMyRightsForStudy(study.id).then(rights => {
            return rights && rights.includes(StudyUserRight.CAN_ADMINISTRATE);
        });
    }

    protected editStudyCard(studycard: StudyCard) {
        let currentStep: Step = this.breadcrumbsService.currentStep;
        this.router.navigate(['/study-card/edit/' + studycard.id]).then(success => {
            this.subscribtions.push(
                currentStep.waitFor(this.breadcrumbsService.currentStep, true).subscribe(entity => {
                    this.importDataService.contextBackup.studyCard = entity as StudyCard;
                })
            );
        });
    }

    ngOnDestroy() {
        for(let subscribtion of this.subscribtions) {
            subscribtion.unsubscribe();
        }
    }
}