package org.joget.gcoe;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    public final static String MESSAGE_PATH = "messages/gcoe";
    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(GovernanceCoeApi.class.getName(), new GovernanceCoeApi(), null));
        registrationList.add(context.registerService(GovernanceCoePluginConfig.class.getName(), new GovernanceCoePluginConfig(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}