package be.kbc.eap.nexus.internal.ui;

import org.sonatype.nexus.rapture.UiPluginDescriptorSupport;

import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
@Priority(Integer.MAX_VALUE - 200)
public class UIPluginDescriptorImpl
        extends UiPluginDescriptorSupport {

    public UIPluginDescriptorImpl() {
        super("nexus-conda-plugin");
        setNamespace("NX.condaui");
        setConfigClassName("NX.condaui.app.PluginConfig");
    }
}
