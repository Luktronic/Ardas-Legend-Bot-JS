package com.ardaslegends.repository;

import com.ardaslegends.domain.ProductionSite;
import com.ardaslegends.domain.ProductionSiteType;
import com.ardaslegends.repository.productionsite.ProductionSiteRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductionSiteRepository extends JpaRepository<ProductionSite, Long>, ProductionSiteRepositoryCustom {

}
