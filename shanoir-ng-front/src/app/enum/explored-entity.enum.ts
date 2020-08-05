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

import { allOfEnum, capitalsAndUnderscoresToDisplayable } from '../utils/app.utils';
import { Option } from '../shared/select/select.component';

export enum ExploredEntity {

    ANATOMICAL_DATASET = 'ANATOMICAL_DATASET',
    FUNCTIONAL_DATASET = 'FUNCTIONAL_DATASET',
    HEMODYNAMIC_DATASET = 'HEMODYNAMIC_DATASET',
    METABOLIC_DATASET = 'METABOLIC_DATASET',
    CALIBRATION = 'CALIBRATION'

} export namespace ExploredEntity {
    
    export function all(): Array<ExploredEntity> {
        return allOfEnum<ExploredEntity>(ExploredEntity);
    }

    export function getLabel(type: ExploredEntity): string {
        return capitalsAndUnderscoresToDisplayable(type);
    }

    export var options: Option<ExploredEntity>[] = all().map(prop => new Option<ExploredEntity>(prop, getLabel(prop)));
}