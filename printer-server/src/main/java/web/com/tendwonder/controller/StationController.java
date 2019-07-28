package com.tendwonder.controller;

import java.awt.Point;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tendwonder.entity.Station;

@RestController
public class StationController {

	@RequestMapping("/selectStation")
	public Station selectStation(Point userLocation) {
		Station station = new Station();
		station.setName("西十一六楼");
		station.setPrinterCode("A001");
		station.setDistance("0.1");
		station.setLocation("越秀区广东工业大学东风路校区生活区");
		return station;
	}
}
