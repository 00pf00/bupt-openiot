package com.bupt.openiot.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.bupt.openiot.conf.OpenIoTServerConfig;
import com.bupt.openiot.dto.BackMessage;
import com.bupt.openiot.dto.DeviceDto;
import com.bupt.openiot.dto.UserInfo;
import com.bupt.openiot.internalsdk.util.HttpClientUtil;

import net.sf.json.JSONObject;

/**
 * Created by dy on 2017/4/21.
 */
@RestController
@RequestMapping("/api")
public class DeviceAccessController {
	
	@Autowired
    private OpenIoTServerConfig openIoTServerConfig;
	

	@RequestMapping(value = "/noauth/deviceaccess")
    public ModelAndView deviceaccessView() {
        ModelAndView result = new ModelAndView("access");
        return result;
    }
    
	/**
	 * @author pf
	 * @param limit 每次请求最多获取的设备个数
	 * @return
	 */
    @SuppressWarnings("unchecked")
	@RequestMapping(value = "/noauth/allDevices" ,method = RequestMethod.GET)
    public List<DeviceDto> getAllDevices( @RequestParam int limit){
    	HttpSession session = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest().getSession();
    	String token = (String) session.getAttribute("token");
    	StringBuffer param = new StringBuffer();
    	param.append("limit").append("=").append(limit);
        String responseContent = HttpClientUtil.getInstance()
                .sendHttpGet("http://" + openIoTServerConfig.getServer()
                        + "/api/tenant/devices", param.toString(), token);
        JSONObject json = JSONObject.fromObject(responseContent);
        Object value = json.get("data");
        List<Map<String, Object>> values = (List<Map<String, Object>>)value;
        //日期转换
        SimpleDateFormat format =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //设备列表
        @SuppressWarnings("rawtypes")
        
		List<DeviceDto>  deviceList = new ArrayList();
        for(Map<String, Object> map:values){
        	DeviceDto device = new DeviceDto();
        	device.setId(((Map<String, Object>)map.get("id")).get("id").toString());       	
        	String date = format.format(new Long( (long) map.get("createdTime")));
        	device.setCreatedTime(date);
        	device.setName((String) map.get("name"));
        	device.setType((String) map.get("type"));
        	if(map.get("additionalInfo").equals("null")){
        		device.setAdditionalInfo("");
        	}else{
        		device.setAdditionalInfo((String) ((Map<String, Object>)map.get("additionalInfo")).get("description"));
        	}
        	deviceList.add(device);
        }
    	return deviceList;
    }
    /**
     * 新增设备并将设备分组，分组是可选项
     * @author pf
     * @param name
     * @param type
     * @param additionInfo
     * @param groupId
     * @return
     */
    @RequestMapping(value = "/noauth/addDevice")
    public BackMessage addDevice(	@RequestParam String name,
    								@RequestParam String type,
    								@RequestParam(required = false) String additionInfo ,
    								@RequestParam(required = false) String groupId){ 
    	BackMessage message = new BackMessage();
    	
    	Map<String,String> map = new HashMap<String, String>();
    	HttpSession session = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest().getSession();
    	map.put("name", name);
    	map.put("type", type);
    	if(additionInfo !=null && !type.trim().equals("")){
    		map.put("additionalInfo", additionInfo);
    	}
    	String responseContent = HttpClientUtil.getInstance().sendHttpPost(
                "http://" + openIoTServerConfig.getServer() + "/api/device", map,(String) session.getAttribute("token"));
    	if(responseContent.trim().equals("") && groupId!= null &&!groupId.trim().equals("")){
    		String deviceId = null;
    		
    		String params = "limit=100";
    		String devices = HttpClientUtil.getInstance()
                    .sendHttpGet("http://" + openIoTServerConfig.getServer()
                            + "/api/tenant/devices", params, (String) session.getAttribute("token"));
    		JSONObject json = JSONObject.fromObject(responseContent);
            Object value = json.get("data");
            List<Map<String, Object>> values = (List<Map<String, Object>>)value;
            for(Map<String, Object> device:values){
            	if((String)device.get("name") ==name){
            		deviceId =  (String)device.get("id");
            	}
            }
            String assignedDevice = HttpClientUtil.getInstance().sendHttpGet("http://" + openIoTServerConfig.getServer()+"/api/group/"+deviceId+"/group/"+groupId, (String) session.getAttribute("token"));
            JSONObject deviceJson = JSONObject.fromObject(assignedDevice);
            message.setObj(deviceJson);
            message.setBackCode("200");
        	message.setBackMessage("success");
        	return message ;
    	}else{
    		message.setBackCode("200");
        	message.setBackMessage("success");
        	return message ;
    	}    	
    }
}
