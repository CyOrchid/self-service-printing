package com.tendwonder.entity;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.tendwonder.base.IdEntity;


@Entity
@Table(name = "station")
public class Station extends IdEntity{

	private String name;
	private String printerCode;
    private String location;        
    private String distance;      
    
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getDistance() {
		return distance;
	}
	public void setDistance(String distance) {
		this.distance = distance;
	}
	public String getPrinterCode() {
		return printerCode;
	}
	public void setPrinterCode(String printerCode) {
		this.printerCode = printerCode;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}