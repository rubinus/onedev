package io.onedev.server.web.page.project.issues.boards;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hibernate.Hibernate;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.IssueManager;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.model.Issue;
import io.onedev.server.model.Milestone;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.issue.TransitionSpec;
import io.onedev.server.model.support.issue.field.spec.FieldSpec;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.util.Input;
import io.onedev.server.web.WebSession;
import io.onedev.server.web.ajaxlistener.AttachAjaxIndicatorListener;
import io.onedev.server.web.ajaxlistener.AttachAjaxIndicatorListener.AttachMode;
import io.onedev.server.web.asset.emoji.Emojis;
import io.onedev.server.web.behavior.AbstractPostAjaxBehavior;
import io.onedev.server.web.component.issue.IssueStateBadge;
import io.onedev.server.web.component.issue.fieldvalues.FieldValuesPanel;
import io.onedev.server.web.component.issue.operation.TransitionMenuLink;
import io.onedev.server.web.component.link.ActionablePageLink;
import io.onedev.server.web.component.modal.ModalLink;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.component.user.ident.Mode;
import io.onedev.server.web.page.base.BasePage;
import io.onedev.server.web.page.project.issues.detail.IssueActivitiesPage;
import io.onedev.server.web.util.Cursor;
import io.onedev.server.web.util.CursorSupport;
import io.onedev.server.web.util.ReferenceTransformer;
import io.onedev.server.web.websocket.WebSocketManager;

@SuppressWarnings("serial")
abstract class BoardCardPanel extends GenericPanel<Issue> {

	private AbstractPostAjaxBehavior ajaxBehavior;
	
	public BoardCardPanel(String id, IModel<Issue> model) {
		super(id, model);
	}

	private Issue getIssue() {
		return getModelObject();
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		List<String> displayFields = ((IssueBoardsPage)getPage()).getBoard().getDisplayFields();
		
		WebMarkupContainer transitLink;
		List<TransitionSpec> transitionSpecs = OneDev.getInstance(SettingManager.class)
				.getIssueSetting().getTransitionSpecs();
		
		if (transitionSpecs.stream().anyMatch(it->it.canTransitManually(getIssue(), null))) {
			transitLink = new TransitionMenuLink("transit") {

				@Override
				protected Issue getIssue() {
					return BoardCardPanel.this.getIssue();
				}
				
			};
			transitLink.add(AttributeAppender.append("class", "transit"));
		} else {
			transitLink = new WebMarkupContainer("transit");
		}
		transitLink.setVisible(displayFields.contains(Issue.NAME_STATE));		
		
		transitLink.add(new IssueStateBadge("state", new AbstractReadOnlyModel<Issue>() {

			@Override
			public Issue getObject() {
				return getIssue();
			}
			
		}));
		
		add(transitLink);
		
		RepeatingView fieldsView = new RepeatingView("fields");
		for (String fieldName: displayFields) {
			if (!fieldName.equals(Issue.NAME_STATE)) {
				Input field = getIssue().getFieldInputs().get(fieldName);
				if (field != null && !field.getType().equals(FieldSpec.USER) && !field.getValues().isEmpty()) {
					fieldsView.add(new FieldValuesPanel(fieldsView.newChildId(), Mode.AVATAR) {

						@Override
						protected Issue getIssue() {
							return BoardCardPanel.this.getIssue();
						}

						@Override
						protected Input getField() {
							if (getIssue().isFieldVisible(fieldName))
								return field;
							else
								return null;
						}

						@SuppressWarnings("deprecation")
						@Override
						protected AttachAjaxIndicatorListener getInplaceEditAjaxIndicator() {
							return new AttachAjaxIndicatorListener(fieldsView.get(fieldsView.size()-1), AttachMode.APPEND, false);
						}
						
					}.setOutputMarkupId(true));
				}
			}
		}
		
		add(fieldsView);
		
		RepeatingView avatarsView = new RepeatingView("avatars");
		for (String fieldName: displayFields) {
			Input field = getIssue().getFieldInputs().get(fieldName);
			if (field != null && field.getType().equals(FieldSpec.USER) && !field.getValues().isEmpty()) {
				avatarsView.add(new FieldValuesPanel(avatarsView.newChildId(), Mode.AVATAR) {

					@SuppressWarnings("deprecation")
					@Override
					protected AttachAjaxIndicatorListener getInplaceEditAjaxIndicator() {
						return new AttachAjaxIndicatorListener(avatarsView.get(0), AttachMode.PREPEND, false);
					}

					@Override
					protected Issue getIssue() {
						return BoardCardPanel.this.getIssue();
					}

					@Override
					protected Input getField() {
						if (getIssue().isFieldVisible(fieldName))
							return field;
						else
							return null;
					}
					
				}.setOutputMarkupId(true));
			}
		}
		
		add(avatarsView);

		BasePage page = (BasePage) getPage();
		
		add(new ModalLink("detail") {

			@Override
			protected String getModalCssClass() {
				return "modal-xl";
			}
			
			private Component newCardDetail(String id, ModalPanel modal, IModel<Issue> issueModel, Cursor cursor) {
				return new CardDetailPanel(id, issueModel) {

					@Override
					protected void onClose(AjaxRequestTarget target) {
						modal.close();
						OneDev.getInstance(WebSocketManager.class).observe(page);
					}

					@Override
					protected CursorSupport<Issue> getCursorSupport() {
						return new CursorSupport<Issue>() {

							@Override
							public Cursor getCursor() {
								return cursor;
							}

							@Override
							public void navTo(AjaxRequestTarget target, Issue entity, Cursor cursor) {
								Long issueId = entity.getId();
								Component cardDetail = newCardDetail(id, modal, new LoadableDetachableModel<Issue>() {

									@Override
									protected Issue load() {
										return OneDev.getInstance(IssueManager.class).load(issueId);
									}
									
								}, cursor);
								
								replaceWith(cardDetail);
								target.add(cardDetail);
							}
							
						};
					}

					@Override
					protected void onDeletedIssue(AjaxRequestTarget target) {
						modal.close();
						OneDev.getInstance(WebSocketManager.class).observe(page);
					}

					@Override
					protected void onAfterRender() {
						OneDev.getInstance(WebSocketManager.class).observe(page);
						super.onAfterRender();
					}

					@Override
					protected Project getProject() {
						return BoardCardPanel.this.getProject();
					}

				};
			}
			
			@Override
			protected Component newContent(String id, ModalPanel modal) {
				return newCardDetail(id, modal, BoardCardPanel.this.getModel(), getCursor());
			}

		});
		
		ActionablePageLink<Void> numberLink;
		add(numberLink = new ActionablePageLink<Void>("number", 
				IssueActivitiesPage.class, IssueActivitiesPage.paramsOf(getIssue())) {

			@Override
			public IModel<?> getBody() {
				String prefix;
				if (getProject().equals(getIssue().getProject()))
					prefix = "";
				else
					prefix = getIssue().getProject().getPath().substring(getProject().getPath().length()+1);
				
				return Model.of(prefix + "#" + getIssue().getNumber());
			}

			@Override
			protected void doBeforeNav(AjaxRequestTarget target) {
				WebSession.get().setIssueCursor(getCursor());
				
				String redirectUrlAfterDelete = RequestCycle.get().urlFor(
						getPage().getClass(), getPage().getPageParameters()).toString();
				WebSession.get().setRedirectUrlAfterDelete(Issue.class, redirectUrlAfterDelete);
			}
			
		});
		
		String url = RequestCycle.get().urlFor(IssueActivitiesPage.class, 
				IssueActivitiesPage.paramsOf(getIssue())).toString();

		ReferenceTransformer transformer = new ReferenceTransformer(getIssue().getProject(), url);
		
		add(new Label("title", Emojis.getInstance().apply(transformer.apply(getIssue().getTitle()))) {

			@Override
			public void renderHead(IHeaderResponse response) {
				super.renderHead(response);
				String script = String.format(""
						+ "$('#%s a:not(.embedded-reference)').click(function() {"
						+ "  $('#%s').click();"
						+ "  return false;"
						+ "});", 
						getMarkupId(), numberLink.getMarkupId());
				response.render(OnDomReadyHeaderItem.forScript(script));
			}
			
		}.setEscapeModelStrings(false).setOutputMarkupId(true));
		
		add(AttributeAppender.append("data-issue", getIssue().getId()));
		
		if (SecurityUtils.getUser() != null)
			add(AttributeAppender.append("style", "cursor:move;"));
		
		add(ajaxBehavior = new AbstractPostAjaxBehavior() {
			
			@Override
			protected void respond(AjaxRequestTarget target) {
				Long issueId = RequestCycle.get().getRequest().getPostParameters()
						.getParameterValue("issue").toLong();
				Issue issue = OneDev.getInstance(IssueManager.class).load(issueId);
				Hibernate.initialize(issue.getProject());
				Hibernate.initialize(issue.getFields());
				Hibernate.initialize(issue.getSubmitter());
				for (Milestone milestone: issue.getMilestones())
					Hibernate.initialize(milestone);
				send(getPage(), Broadcast.BREADTH, new IssueDragging(target, issue));
			}
			
		});
		
		setOutputMarkupId(true);
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		CharSequence callback = ajaxBehavior.getCallbackFunction(CallbackParameter.explicit("issue"));
		String script = String.format("onedev.server.issueBoards.onCardDomReady('%s', %s);", 
				getMarkupId(), SecurityUtils.getUser()!=null?callback:"undefined");
		response.render(OnDomReadyHeaderItem.forScript(script));
	}

	protected abstract Cursor getCursor();
	
	protected abstract Project getProject();
	
}
