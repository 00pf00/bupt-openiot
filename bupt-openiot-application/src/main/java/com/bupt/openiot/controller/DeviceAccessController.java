package com.bupt.openiot.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import com.bupt.openiot.dto.DeviceDto;
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
}
