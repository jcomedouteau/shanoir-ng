import {Component, Input} from '@angular/core';
import {PlannedExecution} from "../models/planned-execution";
import {ModesAware} from "../../preclinical/shared/mode/mode.decorator";
import {EntityComponent, Mode} from "../../shared/components/entity/entity.component.abstract";
import {ActivatedRoute} from "@angular/router";
import {QualityCardService} from "../../study-cards/shared/quality-card.service";
import {StudyService} from "../../studies/shared/study.service";
import {ExaminationService} from "../../examinations/shared/examination.service";
import {StudyRightsService} from "../../studies/shared/study-rights.service";
import {KeycloakService} from "../../shared/keycloak/keycloak.service";
import {CoilService} from "../../coils/shared/coil.service";
import {ConfirmDialogService} from "../../shared/components/confirm-dialog/confirm-dialog.service";
import {PlannedExecutionService} from "./planned-execution.service";
import {FormArray, FormGroup, Validators} from "@angular/forms";
import {StudyCardRulesComponent} from "../../study-cards/study-card-rules/study-card-rules.component";
import {QualityCard} from "../../study-cards/shared/quality-card.model";
import {EntityService} from "../../shared/components/entity/entity.abstract.service";


@Component({
    selector: 'planned-execution',
    templateUrl: './planned-execution.component.html',
    styleUrls: ['./planned-execution.component.css']
})
@ModesAware
export class PlannedExecutionComponent extends EntityComponent<PlannedExecution> {

    constructor(
        private route: ActivatedRoute,
        private studyRightsService: StudyRightsService,
        keycloakService: KeycloakService,
        protected plannedExecutionService: PlannedExecutionService) {
        super(route, 'planned-execution');
    }

    get plannedExecution(): PlannedExecution { return this.entity; }
    set plannedExecution(pe: PlannedExecution) { this.entity= pe; }


    buildForm(): FormGroup {
        return this.formBuilder.group({
            'name': [this.plannedExecution.name, [Validators.required, Validators.minLength(2), this.registerOnSubmitValidator('unique', 'name')]],
        });
    }

    getService(): EntityService<PlannedExecution> {
        return this.plannedExecutionService;
    }

    initCreate(): Promise<void> {
        return Promise.resolve();
    }

    initEdit(): Promise<void> {
        return Promise.resolve();
    }

    initView(): Promise<void> {
        return Promise.resolve();
    }


}
