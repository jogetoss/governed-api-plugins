package org.joget.gcoe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.joget.api.annotations.Operation;
import org.joget.api.annotations.Param;
import org.joget.api.annotations.Response;
import org.joget.api.annotations.Responses;
import org.joget.api.model.ApiDefinition;
import org.joget.api.model.ApiPluginAbstract;
import org.joget.api.model.ApiResponse;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GovernanceCoeApi extends ApiPluginAbstract  {

    @Override
    public String getName() {
        return "GovernedAPI";
    }

    @Override
    public String getVersion() {
        return "7.0-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage(getName() + ".desc", getClassName(), getResourceBundlePath());
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage(getName() + ".label", getClassName(), getResourceBundlePath());
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public String getIcon() {
        return "<i class=\"fas fa-award\"></i>";
    }

    @Override
    public String getTag() {
        return "gcoe";
    }
    
    @Override
    public String getTagDesc() {
        return AppPluginUtil.getMessage(getName() + ".tagDesc", getClassName(), getResourceBundlePath());
    }
    
    @Operation(
        path = "/options/{appId}/{type}",
        type = Operation.MethodType.GET,  
        summary = "@@GovernedAPI.options.summary@@"
    )
    @Responses({
        @Response(responseCode = 200, description = "@@GovernedAPI.resp.200@@", definition = "GCOEOptions", array = true)
    })
    public ApiResponse options(
            HttpServletRequest request,
            @Param(value = "type", required=true, description = "@@GovernedAPI.options.type.desc@@") String type,
            @Param(value = "appId", required=true, description = "@@GovernedAPI.options.appId.desc@@") String appId) {
        
        String apiKey = request.getHeader("api_key");
        
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        
        String apiField = getName("deployments");
        String appIdField = getName("app_ids");
        String condition = " where ("+apiField+" = ? or "+apiField+" like ? or "+apiField+" like ? or "+apiField+" like ?) ";
        condition += " and ("+appIdField+" = ? or "+appIdField+" like ? or "+appIdField+" like ? or "+appIdField+" like ?)";
                        
        Collection<Object> params = new ArrayList<Object>();
        params.add(apiKey);
        params.add(apiKey + ";%");
        params.add("%;" + apiKey + ";%");
        params.add("%;" + apiKey);
        params.add(appId);
        params.add(appId + ";%");
        params.add("%;" + appId + ";%");
        params.add("%;" + appId);
        FormRowSet results = dao.find("permission", "gcoe_permission", condition, params.toArray(new Object[0]), null, null, null, null);
        
        Set<String> configs = new HashSet<String>();
        if (results != null && !results.isEmpty()) {
            for (FormRow r : results) {
                if (r.getProperty("diff").equals("true")) {
                    String insLink = request.getHeader("referer");
                    insLink = insLink.substring(0, insLink.indexOf("/jw") + 3);
                    Collection<Object> paramsIns = new ArrayList<Object>();
                    paramsIns.add(insLink);
                    FormRowSet insResults = dao.find("installations", "gov_installations", "where " + getName("installation_link") + "=?", paramsIns.toArray(new Object[0]), null, null, null, null);
                    for (FormRow insRow : insResults) {
                        String currentIns = insRow.getProperty("id");
                        String multipleInsConfig = r.getProperty("multipleInstallations");
                        try {
                            JSONArray c = new JSONArray(multipleInsConfig);
                            for (int i = 0; i < c.length(); i++) {
                                JSONObject obj = c.getJSONObject(i);
                                if (obj.getString("installation").equals(currentIns)) {
                                    String config = obj.getString("configsForInstallation");
                                    if (config != null && !config.isEmpty()) {
                                        configs.addAll(Arrays.asList(config.split(";")));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LogUtil.error(getClassName(), e, "");
                        }
                    }
                } else {
                    String config = r.getProperty("configs");
                    if (config != null && !config.isEmpty()) {
                        configs.addAll(Arrays.asList(config.split(";")));
                    }
                }
            }
        }
        JSONArray data = new JSONArray();
        
        if (!configs.isEmpty()) {
            String typeField = getName("type");
            condition = " where "+typeField+" = ? and id in ";
            List<String> queries = new ArrayList<String>();
            for (String v : configs) {
                queries.add("?");
            }
            condition += "(" + StringUtils.join(queries, ", ") + ")";
            
            params = new ArrayList<Object>();
            params.add(type);
            params.addAll(configs);
            
            FormRowSet pluginConfigs = dao.find("config", "gcoe_config", condition, params.toArray(new Object[0]), null, null, null, null);
            if (pluginConfigs != null && !pluginConfigs.isEmpty()) {
                try {
                    for (FormRow r : pluginConfigs) {
                        String name = r.getProperty("name");
                        JSONObject rowObject = new JSONObject();
                        rowObject.put("value", r.getId());
                        rowObject.put("label", name);
                        
                        data.put(rowObject);
                    }
                } catch (Exception e) {
                    LogUtil.error(getClassName(), e, "");
                }
            }
        }
        
        return new ApiResponse(200, data);
    }
    
    @Operation(
        path = "/config/{appId}/{id}",
        type = Operation.MethodType.GET,  
        summary = "@@GovernedAPI.config.summary@@"
    )
    @Responses({
        @Response(responseCode = 200, description = "@@GovernedAPI.resp.200@@", definition = "GCOEConfig")
    })
    public ApiResponse config(
            HttpServletRequest request,
            @Param(value = "id", required=true, description = "@@GovernedAPI.config.id.desc@@") String id,
            @Param(value = "appId", required=true, description = "@@GovernedAPI.options.appId.desc@@") String appId) {
        
        String apiKey = request.getHeader("api_key");
        
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        
        String apiField = getName("deployments");
        String appIdField = getName("app_ids");
        String configField = getName("configs");
        String condition = " where ("+apiField+" = ? or "+apiField+" like ? or "+apiField+" like ? or "+apiField+" like ?) ";
        condition += " and ("+appIdField+" = ? or "+appIdField+" like ? or "+appIdField+" like ? or "+appIdField+" like ?)";
        condition += " and ("+configField+" = ? or "+configField+" like ? or "+configField+" like ? or "+configField+" like ?)";
                        
        Collection<Object> params = new ArrayList<Object>();
        params.add(apiKey);
        params.add(apiKey + ";%");
        params.add("%;" + apiKey + ";%");
        params.add("%;" + apiKey);
        params.add(appId);
        params.add(appId + ";%");
        params.add("%;" + appId + ";%");
        params.add("%;" + appId);
        params.add(id);
        params.add(id + ";%");
        params.add("%;" + id + ";%");
        params.add("%;" + id);
        Long count = dao.count("permission", "gcoe_permission", condition, params.toArray(new Object[0]));
        
        JSONObject data = new JSONObject();
        
        if (count > 0) {
            FormRow config = dao.load("config", "gcoe_config", id);
            if (config != null) {
                try {
                    JSONObject c = new JSONObject();
                    c.put("className", config.getProperty("plugin_class"));
                    String props = config.getProperty("plugin_props");
                    props = AppUtil.processHashVariable(props, null, null, null);
                    if (props == null || props.isEmpty()) {
                        props = "{}";
                    }
                    c.put("properties", new JSONObject(props));
                
                    data.put("data", SecurityUtil.encrypt(c.toString()));
                } catch(Exception e) {
                    LogUtil.error(getClassName(), e, "");
                }
            }
        }
        
        return new ApiResponse(200, data);
    }
    
    @Override
    public Map<String, ApiDefinition> getDefinitions() {
        Map<String, ApiDefinition> defs = new HashMap<String, ApiDefinition>();
        
        Map<String, Class> field = new HashMap<String, Class>();
        field.put("value", String.class);
        field.put("label", String.class);
        defs.put("GCOEOptions", new ApiDefinition(field));
        
        Map<String, Class> field2 = new HashMap<String, Class>();
        field2.put("data", String.class);
        defs.put("GCOEConfig", new ApiDefinition(field2));
        
        return defs;
    }
    
    @Override
    public String getResourceBundlePath() {
        return Activator.MESSAGE_PATH;
    }
    
    protected String getName(String name) {
        return "e." + FormUtil.PROPERTY_CUSTOM_PROPERTIES + "." + name;
    }
}