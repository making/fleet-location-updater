package demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.model.CurrentPosition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@SpringBootApplication
//@EnableDiscoveryClient
@EnableCircuitBreaker
@EnableBinding(Processor.class)
@MessageEndpoint
public class FleetLocationUpdaterApplication {

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ServiceLocationService serviceLocationService;

    @ServiceActivator(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
    public Message<String> out(Message<String> in) throws IOException {
        CurrentPosition payload = this.objectMapper.readValue(in.getPayload(), CurrentPosition.class);
        this.serviceLocationService.updateServiceLocations(payload);
        return MessageBuilder
                .withPayload(this.objectMapper.writeValueAsString(payload))
                .copyHeadersIfAbsent(in.getHeaders())
                .build();
    }

    @Bean
    //@LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(FleetLocationUpdaterApplication.class, args);
    }
}
