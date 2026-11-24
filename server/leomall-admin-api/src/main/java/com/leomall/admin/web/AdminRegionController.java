package com.leomall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.leomall.admin.vo.RegionVo;
import com.leomall.core.util.ResponseUtil;
import com.leomall.db.domain.LeomallRegion;
import com.leomall.db.service.LeomallRegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/leomall/admin/region")
@Validated
public class AdminRegionController {
    private final Log logger = LogFactory.getLog(AdminRegionController.class);

    @Autowired
    private LeomallRegionService regionService;

    @GetMapping("/clist")
    public Object clist(@NotNull Integer id) {
        List<LeomallRegion> regionList = regionService.queryByPid(id);
        return ResponseUtil.okList(regionList);
    }

    @GetMapping("/list")
    public Object list() {
        List<RegionVo> regionVoList = new ArrayList<>();

        List<LeomallRegion> leomallRegions = regionService.getAll();
        Map<Byte, List<LeomallRegion>> collect = leomallRegions.stream().collect(Collectors.groupingBy(LeomallRegion::getType));
        byte provinceType = 1;
        List<LeomallRegion> provinceList = collect.get(provinceType);
        byte cityType = 2;
        List<LeomallRegion> city = collect.get(cityType);
        Map<Integer, List<LeomallRegion>> cityListMap = city.stream().collect(Collectors.groupingBy(LeomallRegion::getPid));
        byte areaType = 3;
        List<LeomallRegion> areas = collect.get(areaType);
        Map<Integer, List<LeomallRegion>> areaListMap = areas.stream().collect(Collectors.groupingBy(LeomallRegion::getPid));

        for (LeomallRegion province : provinceList) {
            RegionVo provinceVO = new RegionVo();
            provinceVO.setId(province.getId());
            provinceVO.setName(province.getName());
            provinceVO.setCode(province.getCode());
            provinceVO.setType(province.getType());

            List<LeomallRegion> cityList = cityListMap.get(province.getId());
            List<RegionVo> cityVOList = new ArrayList<>();
            for (LeomallRegion cityVo : cityList) {
                RegionVo cityVO = new RegionVo();
                cityVO.setId(cityVo.getId());
                cityVO.setName(cityVo.getName());
                cityVO.setCode(cityVo.getCode());
                cityVO.setType(cityVo.getType());

                List<LeomallRegion> areaList = areaListMap.get(cityVo.getId());
                List<RegionVo> areaVOList = new ArrayList<>();
                for (LeomallRegion area : areaList) {
                    RegionVo areaVO = new RegionVo();
                    areaVO.setId(area.getId());
                    areaVO.setName(area.getName());
                    areaVO.setCode(area.getCode());
                    areaVO.setType(area.getType());
                    areaVOList.add(areaVO);
                }

                cityVO.setChildren(areaVOList);
                cityVOList.add(cityVO);
            }
            provinceVO.setChildren(cityVOList);
            regionVoList.add(provinceVO);
        }

        return ResponseUtil.okList(regionVoList);
    }
}
