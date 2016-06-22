package demo;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import demo.model.CurrentPosition;
import demo.model.ServiceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ServiceLocationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLocationService.class);

    @Autowired
    //@LoadBalanced
    private RestTemplate restTemplate;

    /**
     * Enriches the provided {@link CurrentPosition} with the closest service location
     * IF the {@link CurrentPosition} has a {@link demo.model.VehicleStatus} of:
     *
     * <ul>
     *   <li>{@link demo.model.VehicleStatus#SERVICE_NOW}</li>
     *   <li>{@link demo.model.VehicleStatus#SERVICE_SOON}</li>
     *   <li>{@link demo.model.VehicleStatus#STOP_NOW}</li>
     * <ul>
     *
     * @param currentPosition Will be enriched with the closest service location
     * @throws Exception
     */
    @HystrixCommand(
            commandKey="serviceLocations",
            commandProperties = {
                    //@HystrixProperty(name = "circuitBreaker.forceOpen", value = "true")
            },
            fallbackMethod = "handleServiceLocationServiceFailure")
    public void updateServiceLocations(CurrentPosition currentPosition) {

        switch (currentPosition.getVehicleStatus()) {

            case SERVICE_NOW:
            case SERVICE_SOON:
            case STOP_NOW:
                ResponseEntity<Resource<ServiceLocation>> result = this.restTemplate.exchange(
                        "http://go-fleet-service-locator.52.68.100.51.xip.io/locationService/{lat}/{long}",
                        HttpMethod.GET, new HttpEntity<>((Void) null),
                        new ParameterizedTypeReference<Resource<ServiceLocation>>() {
                        }, currentPosition.getLocation().getLatitude(),
                        currentPosition.getLocation().getLongitude());
                if (result.getStatusCode() == HttpStatus.OK
                        && result.getBody().getContent() != null) {
                    currentPosition.setServiceLocation(result.getBody().getContent());
                }
                break;
            default:
        }

    }

    public void handleServiceLocationServiceFailure(CurrentPosition currentPosition) {
        LOGGER.error("Hystrix Fallback Method. Unable to retrieve service station info.");
    }

}
