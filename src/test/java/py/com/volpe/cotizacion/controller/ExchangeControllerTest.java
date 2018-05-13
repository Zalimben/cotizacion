package py.com.volpe.cotizacion.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import py.com.volpe.cotizacion.domain.Place;
import py.com.volpe.cotizacion.domain.PlaceBranch;
import py.com.volpe.cotizacion.repository.QueryResponseDetailRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.*;

/**
 * @author Arturo Volpe
 * @since 5/12/18
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ExchangeControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private QueryResponseDetailRepository repo;

    @Test
    public void byIso() {

        Mockito.when(repo.getAvailableISO()).thenReturn(Arrays.asList("EUR", "USD"));

        List<String> data = restTemplate.getForEntity("/api/exchange/", List.class).getBody();

        assertEquals(2, data.size());
        assertThat(data, hasItems("EUR", "USD"));
    }

    @Test
    public void getAvailableCurrencies() {

        Date now = new Date();
        Date tenMinutesAgo = addMinutes(now, -10);
        Mockito.when(repo.getMaxByPlaceInISO("USD")).thenReturn(Arrays.asList(
                new QueryResponseDetailRepository.ByIsoCodeResult(1, new Place(), new PlaceBranch(), 1, now, 1, 5000, 6000),
                new QueryResponseDetailRepository.ByIsoCodeResult(2, new Place(), new PlaceBranch(), 2, tenMinutesAgo, 2, 6000, 7000)
        ));

        ExchangeController.ResultData data = restTemplate.getForEntity("/api/exchange/{cur}", ExchangeController.ResultData.class, "USD").getBody();

        assertEquals(2, data.getCount());
        assertEquals(2, data.getData().size());
        assertThat(data.getData().stream().map(QueryResponseDetailRepository.ByIsoCodeResult::getPurchasePrice).collect(Collectors.toList()), hasItems(6000L, 7000L));
        assertThat(data.getData().stream().map(QueryResponseDetailRepository.ByIsoCodeResult::getSalePrice).collect(Collectors.toList()), hasItems(5000L, 6000L));
        assertEquals(tenMinutesAgo, data.getFirstQueryResult());
        assertEquals(now, data.getLastQueryResult());

        Mockito.when(repo.getMaxByPlaceInISO("EUR")).thenReturn(Collections.emptyList());

        ExchangeController.ResultData dataEUR = restTemplate.getForEntity("/api/exchange/{cur}", ExchangeController.ResultData.class, "EUR").getBody();

        assertEquals(0, dataEUR.getCount());
        assertEquals(0, dataEUR.getData().size());
        assertNull(dataEUR.getFirstQueryResult());
        assertNull(dataEUR.getLastQueryResult());

    }

    private Date addMinutes(Date date, int minutes) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.MINUTE, minutes);
        return c.getTime();
    }
}