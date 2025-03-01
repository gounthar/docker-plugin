package com.nirima.jenkins.plugins.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.dockerjava.api.model.Container;
import hudson.model.Node;
import io.jenkins.docker.DockerTransientNode;
import io.jenkins.docker.client.DockerAPI;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

class DockerContainerWatchdogTest {
    @Test
    void testEmptyEnvironment() throws IOException, InterruptedException {
        TestableDockerContainerWatchdog subject = new TestableDockerContainerWatchdog();

        subject.setAllNodes(new LinkedList<>());
        subject.setAllClouds(new LinkedList<>());

        subject.runExecute();

        assertEquals(0, subject.getAllRemovedNodes().size());
    }

    @Test
    void testSimpleEnvironmentNothingTodo() throws IOException, InterruptedException {
        TestableDockerContainerWatchdog subject = new TestableDockerContainerWatchdog();

        final String nodeName = "unittest-12345";
        final String containerId = UUID.randomUUID().toString();

        /* setup of cloud */
        List<DockerCloud> listOfCloud = new LinkedList<>();

        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(DockerContainerLabelKeys.NODE_NAME, nodeName);
        labelMap.put(DockerContainerLabelKeys.REMOVE_VOLUMES, "false");

        List<Container> containerList = new LinkedList<>();
        Container c = TestableDockerContainerWatchdog.createMockedContainer(containerId, "Running", 0L, labelMap);
        containerList.add(c);

        DockerAPI dockerApi = TestableDockerContainerWatchdog.createMockedDockerAPI(containerList);
        DockerCloud cloud = new DockerCloud("unittestcloud", dockerApi, new LinkedList<>());
        listOfCloud.add(cloud);

        subject.setAllClouds(listOfCloud);

        /* setup of nodes */
        LinkedList<Node> allNodes = new LinkedList<>();

        DockerTransientNode node =
                TestableDockerContainerWatchdog.createMockedDockerTransientNode(containerId, nodeName, cloud, false);
        allNodes.add(node);

        subject.setAllNodes(allNodes);

        subject.runExecute();

        assertEquals(0, subject.getAllRemovedNodes().size());
    }

    @Test
    void testContainerExistsButAgentIsMissing() throws IOException, InterruptedException {
        TestableDockerContainerWatchdog subject = new TestableDockerContainerWatchdog();

        final String nodeName = "unittest-12345";
        final String containerId = UUID.randomUUID().toString();

        /* setup of cloud */
        List<DockerCloud> listOfCloud = new LinkedList<>();

        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(DockerContainerLabelKeys.NODE_NAME, nodeName);
        labelMap.put(DockerContainerLabelKeys.TEMPLATE_NAME, "unittesttemplate");
        labelMap.put(DockerContainerLabelKeys.REMOVE_VOLUMES, "false");

        List<Container> containerList = new LinkedList<>();
        Container c = TestableDockerContainerWatchdog.createMockedContainer(containerId, "Running", 0L, labelMap);
        containerList.add(c);

        DockerAPI dockerApi = TestableDockerContainerWatchdog.createMockedDockerAPI(containerList);
        DockerCloud cloud = new DockerCloud("unittestcloud", dockerApi, new LinkedList<>());
        listOfCloud.add(cloud);

        subject.setAllClouds(listOfCloud);

        /* setup of nodes */
        LinkedList<Node> allNodes = new LinkedList<>();
        subject.setAllNodes(allNodes);

        subject.runExecute();

        assertEquals(0, subject.getAllRemovedNodes().size());

        List<String> containersRemoved = subject.getContainersRemoved();
        assertEquals(1, containersRemoved.size());
        assertEquals(containerId, containersRemoved.get(0));
    }

    @Test
    void testContainerExistsButAgentIsMissingWrongNodeNameIsIgnored() throws IOException, InterruptedException {
        TestableDockerContainerWatchdog subject = new TestableDockerContainerWatchdog();

        final String nodeName = "unittest-12345";
        final String containerId = UUID.randomUUID().toString();

        /* setup of cloud */
        List<DockerCloud> listOfCloud = new LinkedList<>();

        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(DockerContainerLabelKeys.NODE_NAME, nodeName);
        labelMap.put(DockerContainerLabelKeys.TEMPLATE_NAME, "unittesttemplate");
        labelMap.put(DockerContainerLabelKeys.REMOVE_VOLUMES, "false");

        List<Container> containerList = new LinkedList<>();
        Container c = TestableDockerContainerWatchdog.createMockedContainer(containerId, "Running", 0L, labelMap);
        containerList.add(c);

        DockerAPI dockerApi = TestableDockerContainerWatchdog.createMockedDockerAPI(containerList);
        DockerCloud cloud = new DockerCloud("unittestcloud", dockerApi, new LinkedList<>());
        listOfCloud.add(cloud);

        subject.setAllClouds(listOfCloud);

        /* setup of nodes */
        LinkedList<Node> allNodes = new LinkedList<>();

        Node n = TestableDockerContainerWatchdog.createMockedDockerTransientNode(
                UUID.randomUUID().toString(), "unittest-other", cloud, false);
        allNodes.add(n);

        subject.setAllNodes(allNodes);

        subject.runExecute();

        List<String> containersRemoved = subject.getContainersRemoved();
        assertEquals(1, containersRemoved.size());
        assertEquals(containerId, containersRemoved.get(0));

        assertEquals(0, subject.getAllRemovedNodes().size());
    }

    @Test
    void testContainerExistsButAgentIsMissingTwoClouds() throws IOException, InterruptedException {
        TestableDockerContainerWatchdog subject = new TestableDockerContainerWatchdog();

        final String nodeName1 = "unittest-12345";
        final String containerId1 = "12345f63-cce3-4188-82dc-451b444daa40";

        final String nodeName2 = "unittest-12346";
        final String containerId2 = "12346f63-cce3-4188-82dc-451b444daa40";

        /* setup of cloud */
        List<DockerCloud> listOfCloud = new LinkedList<>();

        List<Container> containerList = new LinkedList<>();

        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(DockerContainerLabelKeys.NODE_NAME, nodeName1);
        labelMap.put(DockerContainerLabelKeys.TEMPLATE_NAME, "unittestTemplate");
        labelMap.put(DockerContainerLabelKeys.REMOVE_VOLUMES, "false");

        Container c = TestableDockerContainerWatchdog.createMockedContainer(containerId1, "Running", 0L, labelMap);
        containerList.add(c);

        labelMap = new HashMap<>();
        labelMap.put(DockerContainerLabelKeys.NODE_NAME, nodeName2);
        labelMap.put(DockerContainerLabelKeys.TEMPLATE_NAME, "unittestTemplate");
        labelMap.put(DockerContainerLabelKeys.REMOVE_VOLUMES, "false");

        c = TestableDockerContainerWatchdog.createMockedContainer(containerId2, "Running", 0L, labelMap);
        containerList.add(c);

        DockerAPI dockerApi = TestableDockerContainerWatchdog.createMockedDockerAPI(containerList);
        DockerCloud cloud1 = new DockerCloud("unittestcloud1", dockerApi, new LinkedList<>());
        listOfCloud.add(cloud1);

        DockerCloud cloud2 = new DockerCloud("unittestcloud2", dockerApi, new LinkedList<>());
        listOfCloud.add(cloud2);

        subject.setAllClouds(listOfCloud);

        /* setup of nodes */
        LinkedList<Node> allNodes = new LinkedList<>();
        subject.setAllNodes(allNodes);

        subject.runExecute();

        List<String> containersRemoved = subject.getContainersRemoved();
        assertEquals(4, containersRemoved.size());

        int countContainer1 = 0;
        int countContainer2 = 0;
        for (String containerId : containersRemoved) {
            if (containerId.equals(containerId1)) {
                countContainer1++;
            } else if (containerId.equals(containerId2)) {
                countContainer2++;
            } else {
                fail("Unknown container identifier");
            }
        }

        assertEquals(2, countContainer1);
        assertEquals(2, countContainer2);

        /* NB: Why 2 here?
         * keep in mind that the same containers are associated with the same DockerClient.
         * Thus, the same containers also appear twice to our subject - and thus will send the termination
         * requests twice.
         *
         * Note that this is an acceptable real-life behavior, which is uncommon, though.
         */

        assertEquals(0, subject.getAllRemovedNodes().size());
    }

    @Test
    void testContainerExistsButAgentIsMissingWithTemplate() throws IOException, InterruptedException {
        TestableDockerContainerWatchdog subject = new TestableDockerContainerWatchdog();

        final String nodeName = "unittest-12345";
        final String containerId = UUID.randomUUID().toString();

        /* setup of cloud */
        List<DockerCloud> listOfCloud = new LinkedList<>();

        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(DockerContainerLabelKeys.NODE_NAME, nodeName);
        labelMap.put(DockerContainerLabelKeys.TEMPLATE_NAME, "unittesttemplate");
        labelMap.put(DockerContainerLabelKeys.REMOVE_VOLUMES, "false");

        List<Container> containerList = new LinkedList<>();
        Container c = TestableDockerContainerWatchdog.createMockedContainer(containerId, "Running", 0L, labelMap);
        containerList.add(c);

        DockerAPI dockerApi = TestableDockerContainerWatchdog.createMockedDockerAPI(containerList);
        DockerCloud cloud = new DockerCloud("unittestcloud", dockerApi, new LinkedList<>());
        listOfCloud.add(cloud);

        DockerTemplate template = Mockito.mock(DockerTemplate.class);
        Mockito.when(template.getName()).thenReturn("unittesttemplate");
        Mockito.when(template.isRemoveVolumes()).thenReturn(true);
        cloud.addTemplate(template);

        subject.setAllClouds(listOfCloud);

        /* setup of nodes */
        LinkedList<Node> allNodes = new LinkedList<>();
        subject.setAllNodes(allNodes);

        subject.runExecute();

        Mockito.verify(dockerApi.getClient(), Mockito.times(0))
                .removeContainerCmd(containerId); // enforced termination shall not happen

        assertEquals(0, subject.getAllRemovedNodes().size());

        List<String> containersRemoved = subject.getContainersRemoved();
        assertEquals(1, containersRemoved.size());

        assertEquals(containerId, containersRemoved.get(0));
    }

    @Test
    void testContainerExistsButAgentIsMissingTooEarly() throws IOException, InterruptedException {
        TestableDockerContainerWatchdog subject = new TestableDockerContainerWatchdog();

        Clock clock = Clock.fixed(Instant.ofEpochMilli(1527970544000L), ZoneId.of("UTC"));
        subject.setClock(clock);

        final String nodeName = "unittest-12345";
        final String containerId = UUID.randomUUID().toString();

        /* setup of cloud */
        List<DockerCloud> listOfCloud = new LinkedList<>();

        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(DockerContainerLabelKeys.NODE_NAME, nodeName);
        labelMap.put(DockerContainerLabelKeys.TEMPLATE_NAME, "unittesttemplate");
        labelMap.put(DockerContainerLabelKeys.REMOVE_VOLUMES, "false");

        List<Container> containerList = new LinkedList<>();
        Container c = TestableDockerContainerWatchdog.createMockedContainer(
                containerId, "Running", clock.instant().toEpochMilli() / 1000, labelMap);
        containerList.add(c);

        DockerAPI dockerApi = TestableDockerContainerWatchdog.createMockedDockerAPI(containerList);
        DockerCloud cloud = new DockerCloud("unittestcloud", dockerApi, new LinkedList<>());
        listOfCloud.add(cloud);

        subject.setAllClouds(listOfCloud);

        /* setup of nodes */
        LinkedList<Node> allNodes = new LinkedList<>();
        subject.setAllNodes(allNodes);

        subject.runExecute();

        // shall not have called to remove it by force
        Mockito.verify(dockerApi.getClient(), Mockito.times(0)).removeContainerCmd(containerId);

        // ... and shall not have called to remove it gracefully
        assertEquals(0, subject.getContainersRemoved().size());

        // but, if we turn back time a little...
        subject.setClock(Clock.offset(clock, Duration.ofMinutes(5)));

        subject.runExecute();

        // ... then it should work
        List<String> containersRemoved = subject.getContainersRemoved();
        assertEquals(1, containersRemoved.size());
        assertEquals(containerId, containersRemoved.get(0));
    }

    @Test
    void testContainerExistsButAgentIsMissingRemoveVolumesLabelMissing() throws IOException, InterruptedException {
        TestableDockerContainerWatchdog subject = new TestableDockerContainerWatchdog();

        final String nodeName = "unittest-12345";
        final String containerId = UUID.randomUUID().toString();

        /* setup of cloud */
        List<DockerCloud> listOfCloud = new LinkedList<>();

        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(DockerContainerLabelKeys.NODE_NAME, nodeName);
        labelMap.put(DockerContainerLabelKeys.TEMPLATE_NAME, "unittesttemplate");
        // NB The removeVolumes label is not set here

        List<Container> containerList = new LinkedList<>();
        Container c = TestableDockerContainerWatchdog.createMockedContainer(containerId, "Running", 0L, labelMap);
        containerList.add(c);

        DockerAPI dockerApi = TestableDockerContainerWatchdog.createMockedDockerAPI(containerList);
        DockerCloud cloud = new DockerCloud("unittestcloud", dockerApi, new LinkedList<>());
        listOfCloud.add(cloud);

        subject.setAllClouds(listOfCloud);

        /* setup of nodes */
        LinkedList<Node> allNodes = new LinkedList<>();

        Node n = TestableDockerContainerWatchdog.createMockedDockerTransientNode(
                UUID.randomUUID().toString(), nodeName, cloud, false);
        allNodes.add(n);

        subject.setAllNodes(allNodes);

        subject.runExecute();

        List<String> containersRemoved = subject.getContainersRemoved();
        assertEquals(0, containersRemoved.size());

        assertEquals(0, subject.getAllRemovedNodes().size());
    }

    private static class RemovalTweakingTestableDockerContainerWatchdog extends TestableDockerContainerWatchdog {
        private Clock currentTestClock;

        public RemovalTweakingTestableDockerContainerWatchdog(Clock currentTestClock) {
            super();
            this.currentTestClock = currentTestClock;
        }

        @Override
        protected void removeNode(DockerTransientNode dtn) throws IOException {
            super.removeNode(dtn);

            currentTestClock = Clock.offset(currentTestClock, Duration.ofMinutes(10));
            this.setClock(currentTestClock);
        }

        @Override
        protected boolean stopAndRemoveContainer(
                DockerAPI dockerApi,
                Logger aLogger,
                String description,
                boolean removeVolumes,
                String containerId,
                boolean stop) {
            boolean result =
                    super.stopAndRemoveContainer(dockerApi, aLogger, description, removeVolumes, containerId, stop);

            currentTestClock = Clock.offset(currentTestClock, Duration.ofMinutes(10));
            this.setClock(currentTestClock);

            return result;
        }
    }

    @Test
    void testContainersExistsSlowRemovalShallNotOverspill() throws IOException, InterruptedException {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1527970544000L), ZoneId.of("UTC"));

        TestableDockerContainerWatchdog subject = new RemovalTweakingTestableDockerContainerWatchdog(clock);
        subject.setClock(clock);

        final String nodeName = "unittest-12345";
        final String containerId1 = UUID.randomUUID().toString();
        final String containerId2 = UUID.randomUUID().toString();

        /* setup of cloud */
        List<DockerCloud> listOfCloud = new LinkedList<>();

        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(DockerContainerLabelKeys.NODE_NAME, nodeName);
        labelMap.put(DockerContainerLabelKeys.TEMPLATE_NAME, "unittesttemplate");
        labelMap.put(DockerContainerLabelKeys.REMOVE_VOLUMES, "false");

        List<Container> containerList = new LinkedList<>();
        Container c1 = TestableDockerContainerWatchdog.createMockedContainer(
                containerId1, "Running", clock.instant().toEpochMilli() / 1000, labelMap);
        containerList.add(c1);

        Container c2 = TestableDockerContainerWatchdog.createMockedContainer(
                containerId2, "Running", clock.instant().toEpochMilli() / 1000, labelMap);
        containerList.add(c2);

        DockerAPI dockerApi = TestableDockerContainerWatchdog.createMockedDockerAPI(containerList);
        DockerCloud cloud = new DockerCloud("unittestcloud", dockerApi, new LinkedList<>());
        listOfCloud.add(cloud);

        subject.setAllClouds(listOfCloud);

        /* setup of nodes */
        LinkedList<Node> allNodes = new LinkedList<>();
        subject.setAllNodes(allNodes);

        subject.runExecute();

        // shall not have called to remove it by force
        Mockito.verify(dockerApi.getClient(), Mockito.times(0)).removeContainerCmd(containerId1);

        // ... and shall not have tried to remove neither the first nor the second
        assertEquals(0, subject.getContainersRemoved().size());
    }

    @Test
    void testAgentExistsButNoContainer() throws IOException, InterruptedException {
        TestableDockerContainerWatchdog subject = new TestableDockerContainerWatchdog();

        final String nodeName = "unittest-78901";
        final String containerId = UUID.randomUUID().toString();

        /* setup of cloud */
        List<DockerCloud> listOfCloud = new LinkedList<>();

        List<Container> containerList = new LinkedList<>();

        DockerAPI dockerApi = TestableDockerContainerWatchdog.createMockedDockerAPI(containerList);
        DockerCloud cloud = new DockerCloud("unittestcloud", dockerApi, new LinkedList<>());
        listOfCloud.add(cloud);

        subject.setAllClouds(listOfCloud);

        /* setup of nodes */
        LinkedList<Node> allNodes = new LinkedList<>();

        DockerTransientNode node =
                TestableDockerContainerWatchdog.createMockedDockerTransientNode(containerId, nodeName, cloud, true);
        allNodes.add(node);

        subject.setAllNodes(allNodes);

        subject.runExecute();

        List<DockerTransientNode> nodes = subject.getAllRemovedNodes();
        assertEquals(1, nodes.size());

        DockerTransientNode removedNode = nodes.get(0);
        assertEquals(node, removedNode);
    }

    @Test
    void testAgentExistsButNoContainerCheckForTimeout() throws IOException, InterruptedException {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1527970544000L), ZoneId.of("UTC"));
        TestableDockerContainerWatchdog subject = new RemovalTweakingTestableDockerContainerWatchdog(clock);
        subject.setClock(clock);

        final String nodeName1 = "unittest-78901";
        final String nodeName2 = "unittest-78902";
        final String containerId = UUID.randomUUID().toString();

        /* setup of cloud */
        List<DockerCloud> listOfCloud = new LinkedList<>();

        List<Container> containerList = new LinkedList<>();

        DockerAPI dockerApi = TestableDockerContainerWatchdog.createMockedDockerAPI(containerList);
        DockerCloud cloud = new DockerCloud("unittestcloud", dockerApi, new LinkedList<>());
        listOfCloud.add(cloud);

        subject.setAllClouds(listOfCloud);

        /* setup of nodes */
        LinkedList<Node> allNodes = new LinkedList<>();

        DockerTransientNode node1 =
                TestableDockerContainerWatchdog.createMockedDockerTransientNode(containerId, nodeName1, cloud, true);
        allNodes.add(node1);
        DockerTransientNode node2 =
                TestableDockerContainerWatchdog.createMockedDockerTransientNode(containerId, nodeName2, cloud, true);
        allNodes.add(node2);

        subject.setAllNodes(allNodes);

        subject.runExecute();

        List<DockerTransientNode> nodes = subject.getAllRemovedNodes();
        assertEquals(1, nodes.size());

        DockerTransientNode removedNode = nodes.get(0);
        assertEquals(node1, removedNode);
    }
}
