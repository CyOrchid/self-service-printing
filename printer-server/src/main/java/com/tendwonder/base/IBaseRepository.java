package com.tendwonder.base;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * 自定义拓展接口
 * @param <T>
 * @param <ID>
 */
@NoRepositoryBean
public interface IBaseRepository<T, PK extends Serializable> extends JpaRepository<T, PK> ,JpaSpecificationExecutor<T>{
	
}