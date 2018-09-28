Ext.define('NX.condaui.view.repository.recipe.CondaHosted', {
    extend: 'NX.coreui.view.repository.RepositorySettingsForm',
    alias: 'widget.nx-coreui-repository-conda-hosted',
    requires: [
        'NX.coreui.view.repository.facet.StorageFacet',
        'NX.coreui.view.repository.facet.StorageFacetHosted'
    ],

    initComponent: function() {
        var me = this;

        me.items = [
            { xtype: 'nx-coreui-repository-storage-facet'},
            { xtype: 'nx-coreui-repository-storage-hosted-facet', format: 'conda'}
        ];

        me.callParent();
    }
})