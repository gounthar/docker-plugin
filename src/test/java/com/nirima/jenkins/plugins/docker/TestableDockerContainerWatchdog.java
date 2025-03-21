package com.nirima.jenkins.plugins.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.SlaveComputer;
import io.jenkins.docker.DockerTransientNode;
import io.jenkins.docker.client.DockerAPI;
import java.io.IOException;
import java.time.Clock;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerEndpoint;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

public class TestableDockerContainerWatchdog extends DockerContainerWatchdog {
    private static final String UNITTEST_JENKINS_ID = "f1b65f06-be3e-4dac-a760-b17e7592570f";
    private List<Node> allNodes;
    private List<DockerCloud> allClouds;
    private final List<DockerTransientNode> nodesRemoved = new LinkedList<>();
    private final List<String> containersRemoved = new LinkedList<>();

    public static void setClockOn(DockerContainerWatchdog i, Clock clock) {
        i.setClock(clock);
    }

    @Override
    protected List<DockerCloud> getAllClouds() {
        return List.copyOf(allClouds);
    }

    @Override
    protected List<Node> getAllNodes() {
        return allNodes;
    }

    @Override
    protected void removeNode(DockerTransientNode dtn) throws IOException {
        nodesRemoved.add(dtn);
    }

    @Override
    protected String getJenkinsInstanceId() {
        return UNITTEST_JENKINS_ID;
    }

    @Override
    protected boolean stopAndRemoveContainer(
            DockerAPI dockerApi,
            Logger aLogger,
            String description,
            boolean removeVolumes,
            String containerId,
            boolean stop) {
        containersRemoved.add(containerId);
        return true;
    }

    public void setAllNodes(List<Node> allNodes) {
        this.allNodes = allNodes;
    }

    public void setAllClouds(List<DockerCloud> allClouds) {
        this.allClouds = allClouds;
    }

    public List<DockerTransientNode> getAllRemovedNodes() {
        return List.copyOf(nodesRemoved);
    }

    public List<String> getContainersRemoved() {
        return List.copyOf(containersRemoved);
    }

    public void runExecute() throws IOException, InterruptedException {
        TaskListener mockedListener = Mockito.mock(TaskListener.class);
        Mockito.when(mockedListener.getLogger()).thenReturn(System.out);
        execute(mockedListener);
    }

    public static DockerAPI createMockedDockerAPI(List<Container> containerList) {
        DockerAPI result = Mockito.mock(DockerAPI.class);
        DockerClient client = Mockito.mock(DockerClient.class);
        Mockito.when(result.getClient()).thenReturn(client);
        DockerServerEndpoint dockerServerEndpoint = Mockito.mock(DockerServerEndpoint.class);
        Mockito.when(dockerServerEndpoint.getUri()).thenReturn("tcp://mocked-docker-host:2375");
        Mockito.when(result.getDockerHost()).thenReturn(dockerServerEndpoint);
        ListContainersCmd listContainerCmd = Mockito.mock(ListContainersCmd.class);
        Mockito.when(client.listContainersCmd()).thenReturn(listContainerCmd);
        Mockito.when(listContainerCmd.withShowAll(true)).thenReturn(listContainerCmd);
        Mockito.when(listContainerCmd.withLabelFilter(ArgumentMatchers.anyMap()))
                .thenAnswer((Answer<ListContainersCmd>) invocation -> {
                    Map<String, String> arg = invocation.getArgument(0);
                    String jenkinsInstanceIdInFilter = arg.get(DockerContainerLabelKeys.JENKINS_INSTANCE_ID);
                    assertEquals(UNITTEST_JENKINS_ID, jenkinsInstanceIdInFilter);
                    return listContainerCmd;
                });
        Mockito.when(listContainerCmd.exec()).thenReturn(containerList);
        return result;
    }

    public static Container createMockedContainer(
            String containerId, String status, long createdOn, Map<String, String> labels) {
        Container result = Mockito.mock(Container.class);
        Mockito.when(result.getId()).thenReturn(containerId);
        Mockito.when(result.getStatus()).thenReturn(status);
        Mockito.when(result.getCreated()).thenReturn(createdOn);
        Mockito.when(result.getLabels()).thenReturn(labels);
        return result;
    }

    public static DockerTransientNode createMockedDockerTransientNode(
            String containerId, String nodeName, DockerCloud cloud, boolean offline) {
        DockerTransientNode result = Mockito.mock(DockerTransientNode.class);
        Mockito.when(result.getContainerId()).thenReturn(containerId);
        Mockito.when(result.getNodeName()).thenReturn(nodeName);
        Mockito.when(result.getCloud()).thenReturn(cloud);
        SlaveComputer sc = Mockito.mock(SlaveComputer.class);
        Mockito.when(sc.isOffline()).thenReturn(offline);
        Mockito.when(result.getComputer()).thenReturn(sc);
        return result;
    }
}
