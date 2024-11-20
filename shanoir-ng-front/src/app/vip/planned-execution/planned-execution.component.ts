import {Component, Input} from '@angular/core';
import {PlannedExecution} from "../models/planned-execution";
import {ModesAware} from "../../preclinical/shared/mode/mode.decorator";
import {EntityComponent, Mode} from "../../shared/components/entity/entity.component.abstract";
import {ActivatedRoute} from "@angular/router";
import {StudyRightsService} from "../../studies/shared/study-rights.service";
import {KeycloakService} from "../../shared/keycloak/keycloak.service";
import {PlannedExecutionService} from "./planned-execution.service";
import {FormGroup, UntypedFormControl, UntypedFormGroup, ValidatorFn, Validators} from "@angular/forms";
import {EntityService} from "../../shared/components/entity/entity.abstract.service";
import {PipelineService} from "../pipelines/pipeline/pipeline.service";
import {Pipeline} from "../models/pipeline";
import {PipelineParameter} from "../models/pipelineParameter";
import {ParameterType} from "../models/parameterType";
import {Option} from "../../shared/select/select.component";


@Component({
    selector: 'planned-execution',
    templateUrl: './planned-execution.component.html',
    styleUrls: ['./planned-execution.component.css']
})
@ModesAware
export class PlannedExecutionComponent extends EntityComponent<PlannedExecution> {

    niftiConverters: Option<number>[] = [
        new Option<number>(1, 'DCM2NII_2008_03_31', null, null, null, false),
        new Option<number>(2, 'MCVERTER_2_0_7', null, null, null, false),
        new Option<number>(4, 'DCM2NII_2014_08_04', null, null, null, false),
        new Option<number>(5, 'MCVERTER_2_1_0', null, null, null, false),
        new Option<number>(6, 'DCM2NIIX', null, null, null, false),
        new Option<number>(7, 'DICOMIFIER', null, null, null, false),
        new Option<number>(8, 'MRICONVERTER', null, null, null, false),
    ];

    studyId: number;
    pipelines: Pipeline[]
    pipelineNames: string[]
    selectedPipeline: Pipeline
    executionForm: UntypedFormGroup

    constructor(
        private route: ActivatedRoute,
        private studyRightsService: StudyRightsService,
        keycloakService: KeycloakService,
        private pipelineService: PipelineService,
        protected plannedExecutionService: PlannedExecutionService) {
        super(route, 'planned-execution');
        if ( this.breadcrumbsService.currentStep ) {
            this.studyId = this.breadcrumbsService.currentStep.getPrefilledValue("studyId")
        }
    }

    get plannedExecution(): PlannedExecution { return this.entity; }
    set plannedExecution(pe: PlannedExecution) { this.entity= pe; }

    buildForm(): FormGroup {
        this.executionForm = this.formBuilder.group({
            'name': [this.plannedExecution.name, [Validators.required, Validators.minLength(2), this.registerOnSubmitValidator('unique', 'name')]],
            'examinationNameFilter': [this.plannedExecution.examinationNameFilter],
            'studyId': [this.plannedExecution.studyId, [Validators.required]],
            'vipPipeline': [this.plannedExecution.vipPipeline, [Validators.required]]
        });
        return this.executionForm;
    }

    getService(): EntityService<PlannedExecution> {
        return this.plannedExecutionService;
    }

    initCreate(): Promise<void> {
        this.entity = new PlannedExecution();
        this.entity.studyId = this.studyId;
        this.pipelineService.listPipelines().subscribe(
            (pipelines :Pipeline[])=>{
                this.pipelines = pipelines;
                this.pipelineNames = pipelines.map(pipeline => pipeline.identifier)
            }
        )
        return Promise.resolve()
    }

    initEdit(): Promise<void> {
        this.pipelineService.listPipelines().subscribe(
            (pipelines :Pipeline[])=>{
                this.pipelines = pipelines;
                this.pipelineNames = pipelines.map(pipeline => pipeline.identifier)
            }
        )
        return Promise.resolve();
    }

    initView(): Promise<void> {
        return Promise.resolve();
    }

    isAFile(parameter: PipelineParameter): boolean {
        return parameter.type == ParameterType.File;
    }

    updatePipeline(selectedPipelineIdentifier): void {
        if (selectedPipelineIdentifier) {
            this.pipelineService.getPipeline(selectedPipelineIdentifier).subscribe(pipeline => {
                this.selectedPipeline = pipeline;
                this.selectedPipeline.parameters.forEach(
                    parameter => {
                        let validators: ValidatorFn[] = [];
                        if (!parameter.isOptional && parameter.type != ParameterType.Boolean && parameter.type != ParameterType.File) {
                            validators.push(Validators.required);
                        }
                        let control = new UntypedFormControl(parameter.defaultValue, validators);
                        if (parameter.name != "executable") {
                            this.executionForm.addControl(parameter.name, control);
                        }
                    }
                )
                let groupByControl = new UntypedFormControl("dataset", [Validators.required]);
                this.executionForm.addControl("groupBy", groupByControl);

                let exportControl = new UntypedFormControl("dcm", [Validators.required]);
                this.executionForm.addControl("exportFormat", exportControl);

                let niiConverterControl = new UntypedFormControl(6, []);
                this.executionForm.addControl("niftiConverter", niiConverterControl);
            });

        } else {
            this.selectedPipeline = null;
            this.executionForm = this.buildForm();
        }
    }

}
