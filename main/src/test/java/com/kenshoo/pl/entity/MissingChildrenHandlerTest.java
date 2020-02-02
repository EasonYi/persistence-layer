package com.kenshoo.pl.entity;

import com.google.common.collect.Lists;
import com.kenshoo.pl.entity.internal.ChildrenIdFetcher;
import org.jooq.lambda.Seq;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.contains;
import static org.jooq.lambda.Seq.seq;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by libbyfr on 12/26/2019.
 */
@RunWith(MockitoJUnitRunner.class)
public class MissingChildrenHandlerTest {

    @Mock
    private ChildrenIdFetcher childrenIdFetcher;

    private final TestChildEntity childEntity = TestChildEntity.INSTANCE;
    private final TestEntity parentEntity = TestEntity.INSTANCE;

    private Identifier parentId = new TestEntity.Key(111);
    private Identifier childParentId = new TestChildEntity.ParentId(111);
    private Identifier childId1 = new TestChildEntity.Ordinal(222);
    private Identifier childId2 = new TestChildEntity.Ordinal(333);

    private ChangeFlowConfig flowConfig;
    private MissingChildrenHandler underTest;

    @Before
    public void setUp() {
        underTest = new MissingChildrenHandler(childrenIdFetcher);
        flowConfig = new ChangeFlowConfig.Builder(parentEntity)
                .withChildFlowBuilder(new ChangeFlowConfig.Builder(childEntity))
                .build();
    }

    @Test
    public void when_no_found_MissingChildrenSupplier_then_verify_fetch_from_db_is_not_called() {
        EntityChange parentCmd = new UpdateParent(parentId);

        underTest.handle(Lists.newArrayList(parentCmd), flowConfig);

        verify(childrenIdFetcher, never()).fetch(Lists.newArrayList(parentCmd), childEntity);
    }

    @Test
    public void when_no_return_results_from_db_then_no_call_to_supplyNewCommand_method() {
        EntityChange parentCmd = new UpdateParent(parentId)
                .with(new DeletionOfOther(childEntity));

        setupChildrenInDB(parentCmd, Stream.empty());

        underTest.handle(Lists.newArrayList(parentCmd), flowConfig);

        assertThat(chilrenOf(parentCmd).size(), is(0));
    }

    @Test
    public void when_no_found_childs_in_parent_cmd_then_call_supplyNewCommand_for_each_child_that_returned_from_db() {
        EntityChange parentCmd = new UpdateParent(parentId)
                .with(new DeletionOfOther(childEntity));

        setupChildrenInDB(parentCmd, Seq.of(new FullIdentifier(childParentId, childId1)));

        underTest.handle(Lists.newArrayList(parentCmd), flowConfig);

        ChangeEntityCommand newDeletionCmd = seq(chilrenOf(parentCmd)).filter(c -> c.getChangeOperation() == ChangeOperation.DELETE).findAny().get();
        assertThat(newDeletionCmd.getIdentifier(), is(childId1));
    }

    @Test
    public void when_childCmd_equals_to_childDb_then_no_call_supplyNewCommand_method() {
        ChangeEntityCommand childCmd = new UpdateChild(childId1);
        EntityChange parentCmd = new UpdateParent(parentId)
                .with(new DeletionOfOther(childEntity))
                .with(childCmd);

        setupChildrenInDB(parentCmd, Seq.of(new FullIdentifier(childParentId, childId1)));

        underTest.handle(Lists.newArrayList(parentCmd), flowConfig);

        assertThat(chilrenOf(parentCmd), contains(childCmd));
    }

    @Test
    public void add_child_cmd_to_parent_cmd_for_missing_child() {
        EntityChange parentCmd = new UpdateParent(parentId)
                .with(new DeletionOfOther(childEntity))
                .with(new UpdateChild(childId1));

        setupChildrenInDB(parentCmd, Seq.of(new FullIdentifier(childParentId, childId1), new FullIdentifier(childParentId, childId2)));

        underTest.handle(Lists.newArrayList(parentCmd), flowConfig);

        ChangeEntityCommand newDeletionCmd = seq(chilrenOf(parentCmd)).filter(c -> c.getChangeOperation() == ChangeOperation.DELETE).findAny().get();
        assertThat(newDeletionCmd.getIdentifier(), is(childId2));
    }

    private void setupChildrenInDB(EntityChange parentCmd, Stream<FullIdentifier> fullIdentifiers) {
        when(childrenIdFetcher.fetch(Lists.newArrayList(parentCmd), childEntity)).thenReturn(fullIdentifiers);
    }

    private List<ChangeEntityCommand> chilrenOf(EntityChange parentCmd) {
        return (List<ChangeEntityCommand>) parentCmd.getChildren(childEntity)
                .collect(Collectors.toList());
    }
}