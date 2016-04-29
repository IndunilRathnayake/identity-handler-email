package org.wso2.carbon.identity.email.send.mgt.handler;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.CarbonConfigurationContextFactory;
import org.wso2.carbon.email.mgt.config.Config;
import org.wso2.carbon.email.mgt.config.ConfigBuilder;
import org.wso2.carbon.email.mgt.config.EmailConfigTransformer;
import org.wso2.carbon.email.mgt.dto.EmailTemplateDTO;
import org.wso2.carbon.email.mgt.exceptions.I18nMgtEmailConfigException;
import org.wso2.carbon.event.output.adapter.core.*;
import org.wso2.carbon.event.output.adapter.core.exception.OutputEventAdapterException;
import org.wso2.carbon.event.output.adapter.core.internal.util.EventAdapterConfigHelper;
import org.wso2.carbon.event.output.adapter.email.EmailEventAdapter;
import org.wso2.carbon.event.output.adapter.email.EmailEventAdapterFactory;
import org.wso2.carbon.identity.email.send.mgt.util.Notification;
import org.wso2.carbon.identity.email.send.mgt.util.NotificationBuilder;
import org.wso2.carbon.identity.email.send.mgt.util.NotificationData;
import org.wso2.carbon.identity.event.EventMgtException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.email.mgt.config.ConfigType;
import org.wso2.carbon.email.mgt.config.StorageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EmailSendingHandler extends AbstractEventHandler {

    private static final String EMAIL_NOTIFICATION_TYPE = "EMAIL";

    private static final Log log = LogFactory.getLog(EmailSendingHandler.class);

    @Override
    public boolean handleEvent(Event event) throws EventMgtException {

        String emailType = null;
        String firstName = null;
        Config emailConfig = null;

        Map<String, Object> eventProperties = event.getEventProperties();
        int tenantId = Integer.valueOf((String) eventProperties.get("tenantId"));
        NotificationData emailNotificationData = (NotificationData) eventProperties.get("notificationData");
        String template_type = (String) eventProperties.get("template_type");

        ConfigBuilder configBuilder = ConfigBuilder.getInstance();
        try {
            emailConfig =
                    configBuilder.loadConfiguration(ConfigType.EMAIL,
                            StorageType.REGISTRY,
                            tenantId);
        } catch (Exception e1) {
            throw new EventMgtException(
                    "Could not load the email template configuration for user ", e1);
        }

        try {
            EmailTemplateDTO[] emailTemplateDTOs = EmailConfigTransformer.transform(emailConfig.getProperties());
            EmailTemplateDTO emailTemplateDTO = null;

            for(int i= 0; i< emailTemplateDTOs.length; i++) {
                if(emailTemplateDTOs[i].getName().equals(template_type)) {
                    emailTemplateDTO = emailTemplateDTOs[i];
                }
            }

            Notification emailNotification = null;
            try {
                emailNotification =
                        NotificationBuilder.createNotification(EMAIL_NOTIFICATION_TYPE, emailTemplateDTO,
                                emailNotificationData);
            } catch (Exception e) {
                throw new EventMgtException(
                        "Could not create the email notification for template" + e);
            }
            if (emailNotification == null) {
                throw new IllegalStateException("Notification not set. " +
                        "Please set the notification before sending messages");
            }

            // read parameter from axis2.xml
            AxisConfiguration axisConfiguration =
                    CarbonConfigurationContextFactory.getConfigurationContext()
                            .getAxisConfiguration();
            ArrayList<Parameter> axis_mailParams = axisConfiguration.getTransportOut("mailto").getParameters();
            Map<String, String> globalProperties = new HashMap<String, String>();
            for (Parameter parameter : axis_mailParams) {
                globalProperties.put(parameter.getName(), (String) parameter.getValue());
            }

            EmailEventAdapterFactory emailEventAdapterFactory = new EmailEventAdapterFactory();
            OutputEventAdapter emailEventAdapter = (EmailEventAdapter) emailEventAdapterFactory.createEventAdapter(null, globalProperties);

            //get dynamic properties
            Map<String, String> dynamicProperties = new HashMap<String, String>();
            String emailSubject = emailNotification.getSubject();
            dynamicProperties.put("email.subject", emailSubject);
            String emailType = emailNotification.getType();
            if(emailType == null) {
                emailType = "text/text";
            }
            dynamicProperties.put("email.type", emailType);
            String emailAdd = emailNotification.getSendTo();
            dynamicProperties.put("email.address", emailAdd);

            StringBuilder contents = new StringBuilder();
            contents.append(emailNotification.getBody())
                    .append(System.getProperty("line.separator"))
                    .append(System.getProperty("line.separator"))
                    .append(emailNotification.getFooter());
            String emailBody = contents.toString();

            emailEventAdapter.init();
            emailEventAdapter.connect();
            emailEventAdapter.publish(emailBody, dynamicProperties);

            if (log.isDebugEnabled()) {
                log.debug("Email content : " + emailBody);
            }
            log.info("User credentials configuration mail has been sent to " + emailNotification.getSendTo());
        } catch (I18nMgtEmailConfigException e) {
            log.error("Failed Sending Email", e);
        }
        return true;
    }

    @Override
    public void init() throws EventMgtException {
    }

    @Override
    public String getModuleName() {
        return "emailSend";
    }
}
