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

import { AnimalSubject } from './animalSubject.model';
import { Subject }    from '../../../subjects/shared/subject.model';
import { SubjectPathology } from '../../pathologies/subjectPathology/shared/subjectPathology.model';
import { SubjectTherapy } from '../../therapies/subjectTherapy/shared/subjectTherapy.model';
import { Entity } from "../../../shared/components/entity/entity.abstract";
import {SubjectDTO} from "../../../subjects/shared/subject.dto";

export class PreclinicalSubject extends Entity {
  id: number;
  animalSubject: AnimalSubject;
  subject: Subject;
  pathologies: SubjectPathology[];
  therapies: SubjectTherapy[];
}

export class PreclinicalSubjectDTO {

    id: number;
	animalSubject: AnimalSubject;
    subject: SubjectDTO;
	pathologies: SubjectPathology[];
	therapies: SubjectTherapy[];

    constructor(entity: PreclinicalSubject) {
        this.id = entity.id;
		this.animalSubject = entity.animalSubject;
        this.subject = new SubjectDTO(entity.subject);
		this.pathologies = entity.pathologies;
		this.therapies = entity.therapies;
    }
}
