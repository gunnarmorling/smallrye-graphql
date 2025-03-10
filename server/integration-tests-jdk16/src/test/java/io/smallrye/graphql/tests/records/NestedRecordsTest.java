package io.smallrye.graphql.tests.records;

import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.core.Document;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.vertx.VertxDynamicGraphQLClientBuilder;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.json.JsonValue;
import javax.json.bind.annotation.JsonbCreator;
import java.net.URL;

import static io.smallrye.graphql.client.core.Argument.arg;
import static io.smallrye.graphql.client.core.Argument.args;
import static io.smallrye.graphql.client.core.Document.document;
import static io.smallrye.graphql.client.core.Field.field;
import static io.smallrye.graphql.client.core.InputObject.inputObject;
import static io.smallrye.graphql.client.core.InputObjectField.prop;
import static io.smallrye.graphql.client.core.Operation.operation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(Arquillian.class)
@RunAsClient
public class NestedRecordsTest {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(Parent.class, TestRecord.class);
    }

    @ArquillianResource
    URL testingURL;

    @Test
    public void testNestedRecordWithMissingFieldInQuery() throws Exception {
        try (DynamicGraphQLClient client = new VertxDynamicGraphQLClientBuilder()
                .url(testingURL.toString() + "graphql").build()) {
            Document query = document(operation(
                    field("testParent",
                            args(arg("parent",
                                    inputObject(
                                            prop("testRecord",
                                                    inputObject(prop("needed", "bla")))))),
                            field("testRecord",
                                    field("needed"),
                                    field("notNeeded")))));
            Response response = client.executeSync(query);
            assertEquals("bla", response.getData().getJsonObject("testParent").getJsonObject("testRecord").getString("needed"));
            assertEquals(JsonValue.NULL,
                    response.getData().getJsonObject("testParent").getJsonObject("testRecord").get("notNeeded"));
            assertNull(response.getData().getJsonObject("testParent").get("s"));
        }
    }

    @Test
    public void testSimpleRecordWithMissingFieldInQuery() throws Exception {
        try (DynamicGraphQLClient client = new VertxDynamicGraphQLClientBuilder()
                .url(testingURL.toString() + "graphql").build()) {
            Document query = document(operation(
                    field("echo",
                            args(arg("testRecord",
                                    inputObject(
                                            prop("needed", "bla")))),
                            field("needed"),
                            field("notNeeded"))));
            Response response = client.executeSync(query);
            assertEquals("bla", response.getData().getJsonObject("echo").getString("needed"));
            assertEquals(JsonValue.NULL, response.getData().getJsonObject("echo").get("notNeeded"));
        }
    }

    @GraphQLApi
    public static class Api {

        @Query
        public Parent testParent(Parent parent) {
            return parent;
        }

        @Query
        public TestRecord echo(TestRecord testRecord) {
            return testRecord;
        }

    }

    public record Parent(String s,
            TestRecord testRecord) {
        @JsonbCreator
        public Parent {
        }
    }

    public record TestRecord(String needed,
            String notNeeded) {
        @JsonbCreator
        public TestRecord {
        }
    }

}
