package com.ardaslegends.domain;

import lombok.*;

import jakarta.persistence.*;
import java.util.Objects;


/***
 * Association Table for the Claimbuild 1:n - n:1 ProductionSite Association
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "production_claimbuild")
public final class ProductionClaimbuild extends AbstractDomainObject {

    @EmbeddedId
    private ProductionClaimbuildId id;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @MapsId("productionSiteId")
    @JoinColumn(name = "production_site_id", foreignKey = @ForeignKey(name = "fk_production_claimbuild_production_site_id"))
    private ProductionSite productionSite;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @MapsId("claimbuildId")
    @JoinColumn(name = "claimbuild_id", foreignKey = @ForeignKey(name = "fk_production_claimbuild_claimbuild_id"))
    private ClaimBuild claimbuild;

    private Long count;

    public ProductionClaimbuild(ProductionSite productionSite, ClaimBuild claimbuild, Long count) {
        this.id = new ProductionClaimbuildId();
        this.productionSite = productionSite;
        this.claimbuild = claimbuild;
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductionClaimbuild that = (ProductionClaimbuild) o;
        return productionSite.equals(that.productionSite) && claimbuild.equals(that.claimbuild) && count.equals(that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, productionSite, claimbuild, count);
    }
}
