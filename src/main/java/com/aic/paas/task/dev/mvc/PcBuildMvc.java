package com.aic.paas.task.dev.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.aic.paas.task.dev.peer.PcBuildPeer;

@Controller
@RequestMapping("/dev/buildMvc")
public class PcBuildMvc {
	
	static final Logger logger = LoggerFactory.getLogger(PcBuildMvc.class);

	@Autowired
	PcBuildPeer buildPeer;
	
	/**
	 * aic.tsd_hyh  2016.03.14
	 * 删除构建定义，掉对方接口
	 * @param param
	 * @return
	 */
	@RequestMapping(value="deleteBuild")
	@ResponseBody
	public String delete(@RequestBody String param) {	
		logger.info("---------删除工程-----------task工程------调api接口  入参param："+param);
		String req = buildPeer.removePcBuildApi(param);
		logger.info("---------删除工程-----------task工程------调api接口  回参req："+req);
		return req;
		
		//DemoResponse deresp = new DemoResponse();		
		//deresp.setNamespace("deleteBuild");
		//deresp.setStatus("success");  // "status": "success", //error
		//return JSON.toString(deresp);	
	}
	
	

}
