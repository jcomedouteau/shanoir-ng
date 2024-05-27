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
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Router } from '@angular/router';

import { Selection } from 'src/app/studies/study/study-tree.component';
import { DatasetNode, ProcessingNode } from '../../tree/tree.model';
import { Dataset } from '../shared/dataset.model';
import { DatasetService, Format } from '../shared/dataset.service';
import {MassDownloadService} from "../../shared/mass-download/mass-download.service";
import {TaskState} from "../../async-tasks/task.model";


@Component({
    selector: 'dataset-node',
    templateUrl: 'dataset-node.component.html'
})

export class DatasetNodeComponent implements OnChanges {

    @Input() input: DatasetNode | Dataset;
    @Output() selectedChange: EventEmitter<void> = new EventEmitter();
    node: DatasetNode;
    loading: boolean = false;
    protected menuOpened: boolean = false;
    @Input() hasBox: boolean = false;
    @Input() related: boolean = false;
    detailsPath: string = '/dataset/details/';
    @Output() onDatasetDelete: EventEmitter<void> = new EventEmitter();
    @Input() selection: Selection = new Selection();
    @Input() withMenu: boolean = true;
    public downloadState: TaskState = new TaskState();

    constructor(
        private router: Router,
        private datasetService: DatasetService,
        private downloadService: MassDownloadService) {
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['input']) {
            if (this.input instanceof DatasetNode) {
                this.node = this.input;
            } else {
                throw new Error('not implemented yet');
            }
        }
    }

    toggleMenu() {
        this.menuOpened = this.withMenu && !this.menuOpened;
    }

    download() {
        if (this.loading) {
            return;
        }
        this.loading = true;
        this.downloadService.downloadByIds([this.node.id], this.downloadState)
            .then(() => this.loading = false);
    }

    hasChildren(): boolean | 'unknown' {
        if (this.node.inPacs) {
            return true;
        } else {
            if (!this.node.processings) return false;
            else if (this.node.processings == 'UNLOADED') return 'unknown';
            else return this.node.processings.length > 0;
        }
    }

    deleteDataset() {
        this.datasetService.get(this.node.id).then(entity => {
            this.datasetService.deleteWithConfirmDialog(this.node.title, entity).then(deleted => {
                if (deleted) {
                    this.onDatasetDelete.emit();
                }
            });
        })
    }

    onProcessingDelete(index: number) {
        (this.node.processings as ProcessingNode[]).splice(index, 1) ;
    }
}
