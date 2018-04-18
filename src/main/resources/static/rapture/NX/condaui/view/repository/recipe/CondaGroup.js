Ext.define('NX.condaui.view.repository.recipe.CondaGroup', {
    extend: 'NX.coreui.view.repository.RepositorySettingsForm',
    alias: 'widget.nx-coreui-repository-conda-group',
    requires: [
        'NX.coreui.view.repository.facet.StorageFacet',
        'NX.coreui.view.repository.facet.GroupFacet'
    ],

    initComponent: function() {
        var me = this;

        me.items = [
            { xtype: 'nx-coreui-repository-storage-facet'},
            { xtype: 'nx-coreui-repository-group-facet', format: 'conda'}
        ];

        me.callParent();
    }
})