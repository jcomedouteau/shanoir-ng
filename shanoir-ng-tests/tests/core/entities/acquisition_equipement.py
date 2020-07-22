def test_core_acquisition_equipement(driver, shanoir_util_to_use):
    # Acquisition equipment
    fields = [{
        'name': 'serialNumber',
        'value': '12345',
        'valueEdited': '54321',
        'type': 'text',
        'label': 'Serial number'
    }, {
        'name': 'manufacturerModel',
        'value': 'Achieva 3T',
        'valueEdited': 'Artis Q',
        'type': 'select',
        'label': 'Manufacturer model'
    }, {
        'name': 'center',
        'value': 'CH Colmar',
        'valueEdited': 'CHGR',
        'type': 'select',
        'label': 'Center'
    }]
    menu = ['Medical configuration', 'Acquisition equipments']
    shanoir_util_to_use.test_shanoir_crud_entity(menu, fields)