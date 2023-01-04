package org.joget.gcoe;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.dao.PluginDefaultPropertiesDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PluginDefaultProperties;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBuilderPalette;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.service.UserviewUtil;
import org.joget.commons.util.CsvUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.Plugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.plugin.property.model.PropertyEditable;
import org.joget.plugin.property.service.PropertyUtil;
import org.joget.workflow.util.WorkflowUtil;

public class GovernanceCoePluginConfig extends Element implements FormBuilderPaletteElement, PluginWebSupport {

    @Override
    public String getName() {
        return "GovernedPluginConfig";
    }

    @Override
    public String getVersion() {
        return "7.0-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage(getName() + ".desc", getClassName(), Activator.MESSAGE_PATH);
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage(getName() + ".label", getClassName(), Activator.MESSAGE_PATH);
    }

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "GovernanceCoePluginConfig.ftl";

        // set value
        String value = FormUtil.getElementPropertyValue(this, formData);

        dataModel.put("value", value);
        dataModel.put("url", getServiceUrl());

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<a class='button btn btn-primary'>Configure Plugin</a>";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/form/GovernanceCoePluginConfig.json", null, true, Activator.MESSAGE_PATH);
    }

    @Override
    public String getFormBuilderCategory() {
        return FormBuilderPalette.CATEGORY_GENERAL;
    }

    @Override
    public int getFormBuilderPosition() {
        return 100;
    }

    @Override
    public String getFormBuilderIcon() {
        return "<i class=\"fas fa-plug\"></i>";
    }
    
    protected String getServiceUrl() {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String nonce = SecurityUtil.generateNonce(new String[]{"GovernanceCoePluginConfig", appDef.getId(), appDef.getVersion().toString()}, 3);
        try {
            nonce = URLEncoder.encode(nonce, "UTF-8");
        } catch (Exception e) {}
        
        String url = WorkflowUtil.getHttpServletRequest().getContextPath() + "/web/json/app/"+appDef.getId()+"/"+appDef.getVersion()+"/plugin/org.joget.gcoe.GovernanceCoePluginConfig/service?_nonce=" + nonce;
        return url;
    }
    
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        
        try {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            
            String nonce = request.getParameter("_nonce");
            if (!SecurityUtil.verifyNonce(nonce, new String[]{"GovernanceCoePluginConfig", appDef.getAppId(), appDef.getVersion().toString()})) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            
            if ("config".equals(action)) {
                String submit = request.getParameter("submit");
                String pluginClass = request.getParameter("pluginClass");
                String pluginProperties = request.getParameter("pluginProp");
                
                if ("post".equalsIgnoreCase(request.getMethod()) && "true".equals(submit)) {
                    pluginProperties = request.getParameter("pluginProperties");
                    write("<script>window.parent.updateProps(\""+StringUtil.escapeString(pluginProperties, StringUtil.TYPE_JSON, null)+"\");</script>", response);
                } else {
                    AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
                    PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
                    PluginDefaultPropertiesDao pluginDefaultPropertiesDao = (PluginDefaultPropertiesDao) AppUtil.getApplicationContext().getBean("pluginDefaultPropertiesDao");
                    
                    AppDefinition selectedAppDef = appService.getPublishedAppDefinition(request.getParameter("appId"));
                    Plugin plugin = pluginManager.getPlugin(pluginClass);
                    
                    Map<String, Object> modelMap = new HashMap<String, Object>();
                    modelMap.put("properties", pluginProperties);
                    
                    if (plugin != null) {
                        modelMap.put("propertyEditable", (PropertyEditable) plugin);
                        modelMap.put("plugin", plugin);
                        
                        PluginDefaultProperties pluginDefaultProperties = pluginDefaultPropertiesDao.loadById(pluginClass, selectedAppDef);

                        if (pluginDefaultProperties != null) {
                            if (!(plugin instanceof PropertyEditable)) {
                                Map defaultPropertyMap = new HashMap();

                                String properties = pluginDefaultProperties.getPluginProperties();
                                if (properties != null && properties.trim().length() > 0) {
                                    defaultPropertyMap = CsvUtil.getPluginPropertyMap(properties);
                                }
                                modelMap.put("defaultPropertyMap", defaultPropertyMap);
                            } else {
                                modelMap.put("defaultProperties", PropertyUtil.propertiesJsonLoadProcessing(pluginDefaultProperties.getPluginProperties()));
                            }
                        }
                    }

                    String url = "?"+request.getQueryString() + "&submit=true&action=config";

                    //update nonce in url
                    url = StringUtil.addParamsToUrl(url, "_nonce", nonce);

                    modelMap.put("actionUrl", url);

                    String content = UserviewUtil.renderJspAsString("console/plugin/pluginConfig.jsp", modelMap);
                    content = fixI18nForNoneAdminUser(content);
                    write(fixMissingLabel(content), response);
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }
    
    protected String fixI18nForNoneAdminUser(String content) {
        if(!WorkflowUtil.isCurrentUserInRole("ROLE_ADMIN")) {
            Pattern pattern = Pattern.compile("<script type=\\\"text/javascript\\\" src=\\\".+/web/console/i18n/peditor.+\\\"></script>");
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String script = matcher.group();
                
                Properties keys = new Properties();
                //get message key from property file
                InputStream inputStream = null;
                try {
                    inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("peditor.properties");
                    if (inputStream != null) {
                        keys.load(inputStream);

                        String replace = "<script>var peditor_lang = {";
                        for (Object k : keys.keySet()) {
                            replace += "'"+k.toString()+"' : '" + StringUtil.escapeString(keys.getProperty(k.toString()), StringUtil.TYPE_JSON, null) + "',";
                        }
                        replace += " lang_file_name : 'peditor.properties'}\n";
                        replace += "function get_peditor_msg(key){\n" +
                                   "    return (peditor_lang[key] !== undefined) ? peditor_lang[key] : '??'+key+'??';\n" +
                                   "}\n</script>";
                        
                        content = content.replaceAll(StringUtil.escapeRegex(script), StringUtil.escapeRegex(replace));
                    }
                } catch (Exception e) {
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ex) {
                        }
                    }
                }
            }
        }
        return content;
    } 
    
    protected void write(String content, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.write(content);
    }
    
    protected String fixMissingLabel(String content) {
        content = content.replaceAll(StringUtil.escapeRegex("???"), StringUtil.escapeRegex("@@"));
        PluginManager pluginManager = (PluginManager)AppUtil.getApplicationContext().getBean("pluginManager");
        content = pluginManager.processPluginTranslation(content, getClassName(), null);
        content = content.replaceAll(StringUtil.escapeRegex("@@"), StringUtil.escapeRegex("???"));
        
        return content;
    }
}