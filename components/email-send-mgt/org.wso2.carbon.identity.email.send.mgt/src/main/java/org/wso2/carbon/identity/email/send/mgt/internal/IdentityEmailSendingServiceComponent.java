package org.wso2.carbon.identity.email.send.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.email.send.mgt.handler.EmailSendingHandler;
import org.wso2.carbon.identity.event.handler.EventHandler;

/**
 * @scr.component name="org.wso2.carbon.identity.email.mgt.internal.IdentityAccountLockServiceComponent"
 * immediate="true
 */
public class IdentityEmailSendingServiceComponent {

    private static Log log = LogFactory.getLog(org.wso2.carbon.identity.email.send.mgt.internal.IdentityEmailSendingServiceComponent.class);

    protected void activate(ComponentContext context) {
        context.getBundleContext().registerService(EventHandler.class.getName(),
                new EmailSendingHandler(), null);
        if (log.isDebugEnabled()) {
            log.debug("Identity Management Listener is enabled");
        }
    }


    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Identity Management bundle is de-activated");
        }
    }

}
