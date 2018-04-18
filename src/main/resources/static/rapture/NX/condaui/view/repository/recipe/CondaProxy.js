Ext.define('NX.condaui.view.repository.recipe.CondaProxy', {
    extend: 'NX.coreui.view.repository.RepositorySettingsForm',
    alias: 'widget.nx-coreui-repository-conda-proxy',
    requires: [
        'NX.coreui.view.repository.facet.ProxyFacet',
        'NX.coreui.view.repository.facet.StorageFacet',
        'NX.coreui.view.repository.facet.HttpClientFacet',
        'NX.coreui.view.repository.facet.NegativeCacheFacet'
    ],

    /**
     * @override
     */
    initComponent: function () {
        var me = this;

        me.items = [
            {xtype: 'nx-coreui-repository-proxy-facet'},
            {xtype: 'nx-coreui-repository-storage-facet'},
            {xtype: 'nx-coreui-repository-negativecache-facet'},
            {xtype: 'nx-coreui-repository-httpclient-facet'}
        ];

        me.callParent();
    }
});
