package py.com.volpe.cotizacion.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Arturo Volpe
 * @since 4/26/18
 */
@Data
@Entity
@NoArgsConstructor
@ToString(exclude = "queryResponse")
public class QueryResponseDetail {

    @Id
    @GeneratedValue
    private long id;
    private long purchasePrice;
    private long salePrice;
    private long purchaseTrend;
    private long saleTrend;
    private String isoCode;

    @ManyToOne
    private QueryResponse queryResponse;

    public QueryResponseDetail(long purchasePrice, long salePrice, String isoCode) {
        this.purchasePrice = purchasePrice;
        this.salePrice = salePrice;
        this.isoCode = isoCode;
    }
}
