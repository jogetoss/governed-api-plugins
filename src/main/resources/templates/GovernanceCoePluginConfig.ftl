<div class="form-cell gcoe_button <#if element.properties.label == "">no_label</#if> ${element.properties.id!}" ${elementMetaData!}>
    <input id="${elementParamName!}" name="${elementParamName!}" type="hidden" value="${value!?html}"/>
    <a id="configPlugin" class="button btn btn-primary">${element.properties.label!}</a>
    <script>
        window.updateProps = function(props) {    
            FormUtil.getField("${elementParamName!}").val(props);
            JPopup.hide("GovernanceCoePluginConfig");
        }  
        var url = '${url!}';    

        function configPlugin() {        
            var pluginClass = FormUtil.getValue("${element.properties.pluginClassField!}");        
            var pluginProp = FormUtil.getValue("${elementParamName!}");       
            if (pluginClass !== "") {            
                JPopup.show("GovernanceCoePluginConfig", url, {                
                    "pluginClass": pluginClass,                 
                    "pluginProp": pluginProp,               
                    "action":"config"            
                }, '', '90%%', '90%%', 'post');        
            } else {            
                alert("Please select a plugin to continue.");        
            }    
        }    

        $(document).ready(function(){        
            $("#configPlugin").on("click", function(){            
                configPlugin();            
                return false;        
            });    
        });
    </script>
</div>
