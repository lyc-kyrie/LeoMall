package com.leomall.wx.service;

import com.leomall.db.domain.LeomallRegion;
import com.leomall.db.service.LeomallRegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @date 2019-01-17 23:07
 **/
@Component
public class GetRegionService {

	@Autowired
	private LeomallRegionService regionService;

	private static List<LeomallRegion> leomallRegions;

	protected List<LeomallRegion> getLeomallRegions() {
		if(leomallRegions==null){
			createRegion();
		}
		return leomallRegions;
	}

	private synchronized void createRegion(){
		if (leomallRegions == null) {
			leomallRegions = regionService.getAll();
		}
	}
}
