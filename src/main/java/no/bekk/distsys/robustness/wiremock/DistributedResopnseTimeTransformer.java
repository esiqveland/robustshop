package no.bekk.distsys.robustness.wiremock;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import java.util.Random;

public class DistributedResopnseTimeTransformer extends ResponseTransformer {

    public static final String NAME = "distributed-responsetime-transformer";
    private static Random random = new Random();

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files) {
        Integer minDelay = responseDefinition.getFixedDelayMilliseconds();
        double v = random.nextGaussian();

        int randomDelay = (int) (v * minDelay);
        return ResponseDefinitionBuilder
                .like(responseDefinition)
                .but()
                .withFixedDelay(minDelay + randomDelay)
                .build();
    }

    @Override
    public String name() {
        return NAME; // For reference from stub mappings
    }
}
