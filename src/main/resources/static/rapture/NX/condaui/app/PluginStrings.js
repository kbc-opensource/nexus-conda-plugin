/**
 * Conda plugin strings.
 */
Ext.define('NX.condaui.app.PluginStrings', {
    '@aggregate_priority': 90,

    singleton: true,
    requires: [
        'NX.I18n'
    ],

    keys: {
        Repository_Facet_CondaFacet_Title: 'Conda Settings',
        Repository_Facet_AptFacet_Distribution_FieldLabel: 'Distribution',
        Repository_Facet_AptFacet_Distribution_HelpText: 'Distribution to fetch, for example trusty',
        Repository_Facet_AptFacet_Flat_FieldLabel: 'Flat',
        Repository_Facet_AptFacet_Flat_HelpText: 'Is this repository flat?',
        Repository_Facet_AptSigningFacet_Keypair_FieldLabel: 'Signing Key',
        Repository_Facet_AptSigningFacet_Keypair_HelpText: 'PGP signing key pair',
        Repository_Facet_AptSigningFacet_Passphrase_FieldLabel: 'Passphrase',
        Repository_Facet_AptHostedFacet_AssetHistoryLimit_FieldLabel: 'Asset History Limit',
        Repository_Facet_AptHostedFacet_AssetHistoryLimit_HelpText: 'Number of versions of each package to keep.  If empty all versions will be kept.',
    }
}, function(self) {
    NX.I18n.register(self);
});