package py.com.volpe.cotizacion.gatherer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import py.com.volpe.cotizacion.AppException;
import py.com.volpe.cotizacion.HTTPHelper;
import py.com.volpe.cotizacion.domain.Place;
import py.com.volpe.cotizacion.domain.PlaceBranch;
import py.com.volpe.cotizacion.domain.QueryResponse;
import py.com.volpe.cotizacion.domain.QueryResponseDetail;

import javax.transaction.Transactional;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Arturo Volpe
 * @since 4/26/18
 */
@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class MaxiCambios implements Gatherer {

    private static final String CODE = "MaxiCambios";
    private static final String WS_URL_AS = "http://www.maxicambios.com.py/Umbraco/api/Pizarra/Cotizaciones?fecha=%s";
    private static final String WS_URL_CDE = "http://www.maxicambios.com.py/Umbraco/api/Pizarra/CotizacionesCDE?fecha=%s";

    private final HTTPHelper helper;

    @Override
    public List<QueryResponse> doQuery(Place place, List<PlaceBranch> branches) {

        return branches.stream().map(this::queryBranch).collect(Collectors.toList());

    }

    private QueryResponse queryBranch(PlaceBranch branch) {

        QueryResponse qr = new QueryResponse(branch);

        getParsedData(getURLForBranch(branch)).forEach(detail -> {

            String iso = mapToISO(detail);
            if (iso != null)
                qr.addDetail(mapToDetail(detail, iso));
        });

        return qr;
    }

    private String getURLForBranch(PlaceBranch branch) {
        return "0".equals(branch.getRemoteCode()) ? WS_URL_AS : WS_URL_CDE;
    }

    private QueryResponseDetail mapToDetail(ExchangeData detail, String iso) {
        QueryResponseDetail qrd = new QueryResponseDetail();
        qrd.setIsoCode(iso);
        qrd.setSalePrice(parse(detail.getVenta()));
        qrd.setSaleTrend(detail.isVentaUp() ? 1 : -1);
        qrd.setPurchasePrice(parse(detail.getCompra()));
        qrd.setPurchaseTrend(detail.isCompraUp() ? 1 : -1);
        return qrd;
    }

    @Override
    public String getCode() {
        return CODE;
    }


    /**
     * More branches are present in the main webpage, but it's seems that all the branches shares the same price.
     *
     * @return the newly created place
     */
    @Override
    public Place build() {


        log.info("Creating place {}", CODE);

        Place place = new Place();
        place.setName(CODE);
        place.setCode(CODE);


        PlaceBranch main = new PlaceBranch();
        main.setName("Shopping Multiplaza Casa Central");
        main.setLatitude(-25.3167006);
        main.setLongitude(-57.572267);
        main.setImage("http://www.maxicambios.com.py/media/1044/asuncion_multiplaza.jpg");
        main.setPhoneNumber("(021) 525105/8");
        main.setSchedule("Lunes a Sábados: 8:00 a 21:00 Hs.\n" +
                "Domingos: 10:00 a 21:00 Hs.");
        main.setRemoteCode("0");

        PlaceBranch cde = new PlaceBranch();
        cde.setName("Casa Central CDE");
        cde.setLatitude(-25.5083135);
        cde.setLongitude(-54.6384264);
        cde.setImage("http://www.maxicambios.com.py/media/1072/matriz_cde_original.jpg");
        cde.setPhoneNumber("(061) 573106-574270-574295-509511/13");
        cde.setSchedule("Lunes a viernes: 7:00 a 19:30 Hs. \n Sabados: 7:00 a 12:00 Hs.");
        cde.setRemoteCode("13");


        place.setBranches(Arrays.asList(main, cde));
        return place;

    }

    private ObjectMapper buildMapper() {
        return new ObjectMapper();
    }

    private List<ExchangeData> getParsedData(String wsUrl) {

        try {
            return buildMapper().readValue(getData(wsUrl), new TypeReference<List<ExchangeData>>() {
            });
        } catch (IOException e) {
            throw new AppException(500, "Can't get data from " + CODE, e);
        }
    }

    private String getData(String wsUrl) {

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        String todayStr = sdf.format(new Date());
        return this.helper.doGet(String.format(wsUrl, todayStr));
    }

    private static String mapToISO(ExchangeData data) {
        switch (data.moneda) {
            case "1":
                return "USD";
            case "2":
                return "ARS";
            case "3":
                return "BRL";
            case "4": //URU
                return "UYU";
            case "5": //EURO
                return "EUR";
            case "6": //Mexico
                return "MXN";
            case "7": //Libra
                return "GBP";
            case "8": //Japon
                return "JPY";
            case "9": //Chile
                return "CLP";
            case "18": //Bolivia
                return "BOB";
            case "19": //COlombia
                return "COP";
            case "20": //Peru
                return "PEN";
            default:
                return data.abreviatura;
        }
    }

    @Data
    @JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
    private static class ExchangeData {
        private String abreviatura;
        private String moneda;
        private String rutaBandera;
        private String chCompra;
        private String chCompraUp;
        private String chVenta;
        private String chVentaUp;

        private String img;
        private String compra;
        private boolean compraUp;
        private String venta;
        private boolean ventaUp;
    }

    private static Long parse(String value) {
        return Long.parseLong(value.replaceAll("\\..*", ""));
    }

}
