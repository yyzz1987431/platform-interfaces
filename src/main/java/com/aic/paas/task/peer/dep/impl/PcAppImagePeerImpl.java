package com.aic.paas.task.peer.dep.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.aic.paas.task.bean.dep.AppImageSettings;
import com.aic.paas.task.bean.dep.AppImageSvcInfo;
import com.aic.paas.task.bean.dep.GeneralDeployResp;
import com.aic.paas.task.bean.dep.GeneralReq;
import com.aic.paas.task.bean.dep.GeneralReq.Container;
import com.aic.paas.task.bean.dep.GeneralReq.Container.For;
import com.aic.paas.task.bean.dep.Parameter;
import com.aic.paas.task.bean.dep.PcApp;
import com.aic.paas.task.bean.dep.PcAppImage;
import com.aic.paas.task.bean.dep.PcAppTask;
import com.aic.paas.task.bean.dep.PcKvPair;
import com.aic.paas.task.bean.dev.CPcImage;
import com.aic.paas.task.bean.dev.PcImage;
import com.aic.paas.task.peer.dep.PcAppImagePeer;
import com.aic.paas.task.rest.dep.IDeployServiceManager;
import com.aic.paas.task.rest.dep.PcAppImageSvc;
import com.aic.paas.task.rest.dep.PcAppSvc;
import com.aic.paas.task.rest.dep.PcAppTaskSvc;
import com.aic.paas.task.rest.dev.PcImageSvc;
import com.binary.core.util.BinaryUtils;
import com.binary.json.JSON;

public class PcAppImagePeerImpl implements PcAppImagePeer {

	static final Logger logger = LoggerFactory.getLogger(PcAppImagePeerImpl.class);

	@Autowired
	PcAppImageSvc appImageSvc;

	@Autowired
	PcAppSvc appSvc;

	@Autowired
	PcImageSvc imageSvc;

	@Autowired
	PcAppTaskSvc pcAppTaskSvc;

	@Autowired
	IDeployServiceManager iDeployServiceManager;

	@Override
	public AppImageSettings getAppImageSettings(Long appImageId) {
		AppImageSettings settings = appImageSvc.getAppImageSettings(appImageId);
		if (settings != null) {
			Long imageId = settings.getAppImage().getImageId();
			if (imageId != null) {
				PcImage image = imageSvc.queryImageById(imageId);
				settings.setImage(image);
			}
		}
		return settings;
	}

	@Override
	public List<AppImageSettings> getAppImageSettingsList(Long appId, Long appVnoId) {
		List<AppImageSettings> settingsList = appImageSvc.getAppImageSettingsList(appId, appVnoId);

		if (settingsList != null && settingsList.size() > 0) {
			Set<Long> imageids = new HashSet<Long>();
			for (int i = 0; i < settingsList.size(); i++) {
				AppImageSettings settings = settingsList.get(i);
				imageids.add(settings.getAppImage().getImageId());
			}

			CPcImage imgcdt = new CPcImage();
			imgcdt.setIds(imageids.toArray(new Long[0]));
			List<PcImage> images = imageSvc.queryImageList(imgcdt, null);

			Map<Long, PcImage> imgmap = BinaryUtils.toObjectMap(images, "ID");

			for (int i = 0; i < settingsList.size(); i++) {
				AppImageSettings settings = settingsList.get(i);
				Long imageId = settings.getAppImage().getImageId();
				PcImage image = imgmap.get(imageId);
				settings.setImage(image);
			}
		}

		return settingsList;
	}

	@Override
	public String startDeploy(Long appId, Long appVnoId) {
		logger.info("appId ++++:" + appId);
		logger.info("appVnoId" + appVnoId);
		List<AppImageSettings> appImageList = getAppImageSettingsList(appId, appVnoId);

		PcApp pcApp = appSvc.queryById(appId);

		GeneralReq generalReq = new GeneralReq();
		generalReq.setAppId(pcApp.getId().toString());
		generalReq.setAppName(pcApp.getAppCode());
		generalReq.setAppNameCN(pcApp.getAppName());
		generalReq.setClusterId(pcApp.getResCenterId().toString());
		// generalReq.setClusterName(clusterName)
		generalReq.setDataCenterId(pcApp.getDataCenterId().toString());

		List<Container> containers = new ArrayList<Container>();
		for (AppImageSettings setting : appImageList) {
			Container container = new Container();
			container.setContainerId(setting.getAppImage().getId().toString());
			container.setContainerName(setting.getAppImage().getContainerName());
			container.setZoneId(setting.getAppImage().getNetZoneId().toString());
			List<Parameter> attrs = new ArrayList<Parameter>();
			List<PcKvPair> paramList = setting.getParams();
			for (PcKvPair pair : paramList) {
				Parameter pa = new Parameter();
				pa.setKey(pair.getKvKey());
				pa.setValue(pair.getKvVal());
			}
			container.setAttrs(attrs);
			container.setVersion(pcApp.getVersionNo());
			List<PcAppImage> dependList = setting.getDependImages();

			List<String> containeIdList = new ArrayList<String>();
			for (PcAppImage image : dependList) {
				containeIdList.add(image.getId().toString());
			}
			container.setImgFullName("redis");
			container.setImgVersion("3.0.6");
			// crontainer.setDepends(containeIdList.toArray().toString());
			container.setCpu(setting.getAppImage().getCpuCount() / 100.0);
			container.setMem(Integer.parseInt(setting.getAppImage().getMemSize().toString()));
			// 硬盘大小单位是GB 需要转换成MB X1024
			container.setDisk(Integer.parseInt(setting.getAppImage().getDiskSize().toString()) * 1024);
			container.setInstances(setting.getAppImage().getInstanceCount());
			// containe.set
			if (setting.getAppImage().getIsSupportFlex().equals(1)) {

				container.setMaxInst(setting.getAppImage().getMaxInstanceCount());
				container.setMinInst(setting.getAppImage().getMinInstanceCount());
				container.setScaleInCpu(setting.getAppImage().getCpuFlexLowerLimit());
				container.setScaleOutCpu(setting.getAppImage().getCpuFlexUpperLimit());
			}

			List<For> servicesFor = new ArrayList<For>();
			List<AppImageSvcInfo> callServiceList = setting.getCallServices();
			for (AppImageSvcInfo imageSvcInfo : callServiceList) {
				imageSvcInfo.getSvc();

			}

			container.setServicesFor(servicesFor);
			container.setLogDir(setting.getAppImage().getLogMpPath());
			container.setDataDir(setting.getAppImage().getDataMpPath());
			// 拼装json数据
			containers.add(container);
		}
		generalReq.setContainers(containers);

		System.out.println(JSON.toString(generalReq));
		String resStr = iDeployServiceManager.createLongRun(JSON.toString(generalReq));
		GeneralDeployResp resp = JSON.toObject(resStr, GeneralDeployResp.class);
		if (GeneralDeployResp.SUCCESS.equals(resp.getResultCode())) {
			// write task log and so on...
			PcAppTask pcAppTask = new PcAppTask();
			pcAppTask.setId(resp.getReqId().longValue());
			pcAppTask.setAppId(appId);
			pcAppTaskSvc.saveOrUpdate(pcAppTask);
		}
		System.out.println(JSON.toString(resStr));
		return resStr;

	}

	@Override
	public String reDeploy(Long appId, Long appVnoId) {
		List<AppImageSettings> appImageList = getAppImageSettingsList(appId, appVnoId);
		PcApp pcApp = appSvc.queryById(appId);

		GeneralReq generalReq = new GeneralReq();
		generalReq.setAppId(pcApp.getId().toString());
		generalReq.setAppName(pcApp.getAppCode());
		generalReq.setAppNameCN(pcApp.getAppName());
		generalReq.setClusterId(pcApp.getResCenterId().toString());
		List<Container> containers = new ArrayList<Container>();
		for (AppImageSettings setting : appImageList) {
			Container container = new Container();
			container.setContainerId(setting.getAppImage().getId().toString());
			container.setContainerName(setting.getAppImage().getContainerName());
			container.setInstances(setting.getAppImage().getInstanceCount());
			container.setImgFullName("redis");
			container.setImgVersion("3.0.5");
			containers.add(container);
		}
		generalReq.setContainers(containers);
		String resStr = iDeployServiceManager.upgrade(JSON.toString(generalReq));
		GeneralDeployResp resp = JSON.toObject(resStr, GeneralDeployResp.class);
		if (GeneralDeployResp.SUCCESS.equals(resp.getResultCode())) {
			// write task log and so on...
			PcAppTask pcAppTask = new PcAppTask();
			pcAppTask.setId(resp.getReqId().longValue());
			pcAppTask.setAppId(appId);
			pcAppTaskSvc.saveOrUpdate(pcAppTask);
		}
		return resStr;
	}

	@Override
	public String destroyDeploy(Long appId, Long appVnoId) {
		List<AppImageSettings> appImageList = getAppImageSettingsList(appId, appVnoId);
		PcApp pcApp = appSvc.queryById(appId);

		GeneralReq generalReq = new GeneralReq();
		generalReq.setAppId(pcApp.getId().toString());
		generalReq.setAppName(pcApp.getAppCode());
		generalReq.setAppNameCN(pcApp.getAppName());
		generalReq.setClusterId(pcApp.getResCenterId().toString());
		List<Container> containers = new ArrayList<Container>();
		for (AppImageSettings setting : appImageList) {
			Container container = new Container();
			container.setContainerId(setting.getAppImage().getId().toString());
			container.setContainerName(setting.getAppImage().getContainerName());
			containers.add(container);
		}
		generalReq.setContainers(containers);
		String resStr = iDeployServiceManager.destroyLongRun(JSON.toString(generalReq));
		GeneralDeployResp resp = JSON.toObject(resStr, GeneralDeployResp.class);
		if (GeneralDeployResp.SUCCESS.equals(resp.getResultCode())) {
			// write task log and so on...
			PcAppTask pcAppTask = new PcAppTask();
			pcAppTask.setId(resp.getReqId().longValue());
			pcAppTask.setAppId(appId);
			pcAppTaskSvc.saveOrUpdate(pcAppTask);
		}
		return resStr;
	}

	@Override
	public String startApp(Long appId, Long appVnoId) {
		List<AppImageSettings> appImageList = getAppImageSettingsList(appId, appVnoId);
		PcApp pcApp = appSvc.queryById(appId);

		GeneralReq generalReq = new GeneralReq();
		generalReq.setAppId(pcApp.getId().toString());
		generalReq.setAppName(pcApp.getAppCode());
		generalReq.setAppNameCN(pcApp.getAppName());
		generalReq.setClusterId(pcApp.getResCenterId().toString());
		List<Container> containers = new ArrayList<Container>();
		for (AppImageSettings setting : appImageList) {
			Container container = new Container();
			container.setContainerId(setting.getAppImage().getId().toString());
			container.setContainerName(setting.getAppImage().getContainerName());
			container.setInstances(setting.getAppImage().getInstanceCount());
			containers.add(container);
		}
		generalReq.setContainers(containers);
		String resStr = iDeployServiceManager.start(JSON.toString(generalReq));
		GeneralDeployResp resp = JSON.toObject(resStr, GeneralDeployResp.class);
		if (GeneralDeployResp.SUCCESS.equals(resp.getResultCode())) {
			// write task log and so on...
			PcAppTask pcAppTask = new PcAppTask();
			pcAppTask.setId(resp.getReqId().longValue());
			pcAppTask.setAppId(appId);
			pcAppTaskSvc.saveOrUpdate(pcAppTask);
		}
		return resStr;
	}

	@Override
	public String pauseApp(Long appId, Long appVnoId) {
		List<AppImageSettings> appImageList = getAppImageSettingsList(appId, appVnoId);
		PcApp pcApp = appSvc.queryById(appId);

		GeneralReq generalReq = new GeneralReq();
		generalReq.setAppId(pcApp.getId().toString());
		generalReq.setAppName(pcApp.getAppCode());
		generalReq.setAppNameCN(pcApp.getAppName());
		generalReq.setClusterId(pcApp.getResCenterId().toString());
		List<Container> containers = new ArrayList<Container>();
		for (AppImageSettings setting : appImageList) {
			Container container = new Container();
			container.setContainerId(setting.getAppImage().getId().toString());
			container.setContainerName(setting.getAppImage().getContainerName());
			container.setInstances(0);
			containers.add(container);
		}
		generalReq.setContainers(containers);
		String resStr = iDeployServiceManager.stop(JSON.toString(generalReq));
		GeneralDeployResp resp = JSON.toObject(resStr, GeneralDeployResp.class);
		if (GeneralDeployResp.SUCCESS.equals(resp.getResultCode())) {
			// write task log and so on...
			PcAppTask pcAppTask = new PcAppTask();
			pcAppTask.setId(resp.getReqId().longValue());
			pcAppTask.setAppId(appId);
			pcAppTaskSvc.saveOrUpdate(pcAppTask);
		}
		return resStr;
	}

}
