package no.bekk.distsys.robustness.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

@Headers({
        "Content-Type: application/json",
        "Accept: application/json"
})
public interface GithubFeign {

    @RequestLine("GET /repos/{owner}/{repo}/contributors")
    List<Contributor> contributors(@Param("owner") String owner, @Param("repo") String repo);

    static class Contributor {
        @JsonProperty
        String login;
        @JsonProperty
        int contributions;
    }
}
