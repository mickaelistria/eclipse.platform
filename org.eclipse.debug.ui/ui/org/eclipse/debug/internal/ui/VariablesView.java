package org.eclipse.debug.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.help.ViewContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * This view shows variables and their values for a particular stack frame
 */
public class VariablesView extends AbstractDebugView implements ISelectionListener, 
																	IDoubleClickListener, 
																	IPropertyChangeListener {

	/**
	 * Actions hosted in this view, either in the toolbar, or in one of the 
	 * context menus.
	 */
	private ShowQualifiedAction fShowQualifiedAction;
	private ShowTypesAction fShowTypesAction;
	private ChangeVariableValueAction fChangeVariableAction;
	private AddToInspectorAction fAddToInspectorAction;
	private ControlAction fCopyToClipboardAction;
	private ShowVariableDetailPaneAction fShowDetailPaneAction;
	
	/**
	 * The model presentation used as the label provider for the tree viewer,
	 * and also as the detail information provider for the detail pane.
	 */
	private IDebugModelPresentation fModelPresentation;
	
	/**
	 * The UI construct that provides a sliding sash between the variables tree
	 * and the detail pane.
	 */
	private SashForm fSashForm;
	
	/**
	 * The detail pane viewer and its associated document.
	 */
	private TextViewer fDetailTextViewer;
	private IDocument fDetailDocument;
	
	/**
	 * Various listeners used to update the enabled state of actions and also to
	 * populate the detail pane.
	 */
	private ISelectionChangedListener fTreeSelectionChangedListener;
	private ISelectionChangedListener fDetailSelectionChangedListener;
	private IDocumentListener fDetailDocumentListener;
	
	/**
	 * Collections for tracking actions.
	 */
	private Map fGlobalActions= new HashMap(3);	
	private List fSelectionActions = new ArrayList(3);
	
	/**
	 * These are used to initialize and persist the position of the sash that
	 * separates the tree viewer from the detail pane.
	 */
	private static final int[] DEFAULT_SASH_WEIGHTS = {6, 2};
	private int[] fLastSashWeights;
	private boolean fToggledDetailOnce;

	public VariablesView() {
		DebugUIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);		
	}
	
	/**
	 * Remove myself as a selection listener to the <code>LaunchesView</code> in this perspective,
	 * and remove myself as a preference change listener.
	 *
	 * @see IWorkbenchPart#dispose()
	 */
	public void dispose() {
		DebugUIPlugin.getDefault().removeSelectionListener(this);
		DebugUIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		super.dispose();
	}

	/** 
	 * The <code>VariablesView</code> listens for selection changes in the <code>LaunchesView</code>
	 *
	 * @see ISelectionListener#selectionChanged(IWorkbenchPart, ISelection)
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection sel) {
		if (part instanceof LaunchesView) {
			if (sel instanceof IStructuredSelection) {
				setViewerInput((IStructuredSelection)sel);
			}
		}
		if (!(part instanceof DebugView)) {
			return;
		}
		if (!(sel instanceof IStructuredSelection)) {
			return;
		}

		setViewerInput((IStructuredSelection)sel);
	}

	protected void setViewerInput(IStructuredSelection ssel) {
		IStackFrame frame= null;
		if (ssel.size() == 1) {
			Object input= ssel.getFirstElement();
			if (input != null && input instanceof IStackFrame) {
				frame= (IStackFrame)input;
			}
		}

		Object current= getViewer().getInput();
		if (current == null && frame == null) {
			return;
		}

		if (current != null && current.equals(frame)) {
			return;
		}

		((VariablesContentProvider)getViewer().getContentProvider()).clearCache();
		getViewer().setInput(frame);
	}
	
	/**
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String propertyName= event.getProperty();
		if (!propertyName.equals(IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_ORIENTATION)) {
			return;
		}
		setDetailPaneOrientation((String)event.getNewValue());
	}
	
	/**
	 * @see IWorkbenchPart#createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {
		DebugUIPlugin.getDefault().addSelectionListener(this);
		fModelPresentation = new DelegatingModelPresentation();
		
		// create the sash form that will contain the tree viewer & text viewer
		fSashForm = new SashForm(parent, SWT.NONE);
		IPreferenceStore prefStore = DebugUIPlugin.getDefault().getPreferenceStore();
		String orientString = prefStore.getString(IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_ORIENTATION);
		setDetailPaneOrientation(orientString);
		
		// add tree viewer
		TreeViewer vv = new TreeViewer(getSashForm(), SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		setViewer(vv);
		getViewer().setContentProvider(new VariablesContentProvider());
		getViewer().setLabelProvider(getModelPresentation());
		getViewer().setUseHashlookup(true);
		getViewer().addDoubleClickListener(this);
		
		// add text viewer
		fDetailTextViewer = new TextViewer(getSashForm(), SWT.V_SCROLL | SWT.H_SCROLL);
		getDetailTextViewer().setDocument(getDetailDocument());
		getDetailDocument().addDocumentListener(getDetailDocumentListener());
		getDetailTextViewer().setEditable(false);
		getSashForm().setMaximizedControl(getViewer().getControl());
		getViewer().addSelectionChangedListener(getTreeSelectionChangedListener());
		getDetailTextViewer().getSelectionProvider().addSelectionChangedListener(getDetailSelectionChangedListener());
		
		// add a context menu to the tree
		createTreeContextMenu(vv.getTree());
		
		// add a context menu to the detail area
		createDetailContextMenu(getDetailTextViewer().getTextWidget());

		initializeActions();
		initializeToolBar();
	
		setInitialContent();
		setTitleToolTip(DebugUIMessages.getString("VariablesView.Variables_and_their_Values_for_a_Selected_Stack_Frame_1")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(parent,
			new ViewContextComputer(this, IDebugHelpContextIds.VARIABLE_VIEW ));
	}
	
	/**
	 * Set the orientation of the sash form to display its controls according to the value
	 * of <code>value</code>.  'VARIABLES_DETAIL_PANE_UNDERNEATH' means that the detail 
	 * pane appears underneath the tree viewer, 'VARIABLES_DETAIL_PANE_RIGHT' means the
	 * detail pane appears to the right of the tree viewer.
	 */
	protected void setDetailPaneOrientation(String value) {
		int orientation = value.equals(IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_UNDERNEATH) ? SWT.VERTICAL : SWT.HORIZONTAL;
		getSashForm().setOrientation(orientation);				
	}
	
	/**
	 * Show or hide the detail pane, based on the value of <code>on</code>.
	 * If showing, reset the sash form to use the relative weights that were
	 * in effect the last time the detail pane was visible, and populate it with
	 * details for the current selection.  If hiding, save the current relative 
	 * weights, unless the detail pane hasn't yet been shown.
	 */
	protected void toggleDetailPane(boolean on) {
		if (on) {
			getSashForm().setMaximizedControl(null);
			getSashForm().setWeights(getLastSashWeights());
			IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
			populateDetailPaneFromSelection(selection);
			fToggledDetailOnce = true;
		} else {
			if (fToggledDetailOnce) {
				setLastSashWeights(getSashForm().getWeights());
			}
			getSashForm().setMaximizedControl(getViewer().getControl());
		}
	}
	
	/**
	 * Return the relative weights that were in effect the last time both panes were
	 * visible in the sash form, or the default weights if both panes have not yet been
	 * made visible.
	 */
	protected int[] getLastSashWeights() {
		if (fLastSashWeights == null) {
			fLastSashWeights = DEFAULT_SASH_WEIGHTS;
		}
		return fLastSashWeights;
	}
	
	/**
	 * Set the current relative weights of the controls in the sash form, so that
	 * the sash form can be reset to this layout at a later time.
	 */
	protected void setLastSashWeights(int[] weights) {
		fLastSashWeights = weights;
	}

	protected void setInitialContent() {
		IWorkbenchWindow window= DebugUIPlugin.getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}
		IWorkbenchPage p= window.getActivePage();
		if (p == null) {
			return;
		}
		DebugView view= (DebugView) p.findView(IDebugUIConstants.ID_DEBUG_VIEW);
		if (view != null) {
			ISelectionProvider provider= view.getSite().getSelectionProvider();
			if (provider != null) {
				provider.getSelection();
				ISelection selection= provider.getSelection();
				if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
					setViewerInput((IStructuredSelection) selection);
				}
			}
		}
	}
	
	/**
	 * Create the context menu particular to the tree pane.  Note that anyone
	 * wishing to contribute an action to this menu must use
	 * <code>IDebugUIConstants.VARIABLE_VIEW_VARIABLE_ID</code> as the
	 * <code>targetID</code> in the extension XML.
	 */
	protected void createTreeContextMenu(Control menuControl) {
		MenuManager menuMgr= new MenuManager(); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				fillContextMenu(mgr);
			}
		});
		Menu menu= menuMgr.createContextMenu(menuControl);
		menuControl.setMenu(menu);

		// register the context menu such that other plugins may contribute to it
		getSite().registerContextMenu(IDebugUIConstants.VARIABLE_VIEW_VARIABLE_ID, menuMgr, getViewer());
	}

	/**
	 * Create the context menu particular to the detail pane.  Note that anyone
	 * wishing to contribute an action to this menu must use
	 * <code>IDebugUIConstants.VARIABLE_VIEW_DETAIL_ID</code> as the
	 * <code>targetID</code> in the extension XML.
	 */
	protected void createDetailContextMenu(Control menuControl) {
		MenuManager menuMgr= new MenuManager(); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				fillDetailContextMenu(mgr);
			}
		});
		Menu menu= menuMgr.createContextMenu(menuControl);
		menuControl.setMenu(menu);

		// register the context menu such that other plugins may contribute to it
		getSite().registerContextMenu(IDebugUIConstants.VARIABLE_VIEW_DETAIL_ID, menuMgr, getDetailTextViewer().getSelectionProvider());		
	}
	
	/**
	 * Initializes ALL actions for the toolbar and both context menus.
	 */
	protected void initializeActions() {
		setShowTypesAction(new ShowTypesAction(getViewer()));
		getShowTypesAction().setChecked(false);
		
		setShowQualifiedAction(new ShowQualifiedAction(getViewer()));
		getShowQualifiedAction().setChecked(false);
		
		setAddToInspectorAction(new AddToInspectorAction(getViewer()));
		
		setChangeVariableAction(new ChangeVariableValueAction(getViewer()));
		getChangeVariableAction().setEnabled(false);
		
		setCopyToClipboardAction(new ControlAction(getViewer(), new CopyVariablesToClipboardActionDelegate()));
		
		setShowDetailPaneAction(new ShowVariableDetailPaneAction(this));
		getShowDetailPaneAction().setChecked(false);
	
		IActionBars actionBars= getViewSite().getActionBars();
		TextViewerAction action= new TextViewerAction(getDetailTextViewer(), getDetailTextViewer().getTextOperationTarget().COPY);
		action.configureAction(DebugUIMessages.getString("ConsoleView.&Copy@Ctrl+C_6"), DebugUIMessages.getString("ConsoleView.Copy_7"), DebugUIMessages.getString("ConsoleView.Copy_8")); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		setGlobalAction(actionBars, ITextEditorActionConstants.COPY, action);

		//XXX Still using "old" resource access
		ResourceBundle bundle= ResourceBundle.getBundle("org.eclipse.debug.internal.ui.DebugUIMessages"); //$NON-NLS-1$
		setGlobalAction(actionBars, ITextEditorActionConstants.FIND, new FindReplaceAction(bundle, "find_replace_action.", this));	 //$NON-NLS-1$
	
		fSelectionActions.add(ITextEditorActionConstants.COPY);
		updateAction(ITextEditorActionConstants.FIND);
	} 

	protected void setGlobalAction(IActionBars actionBars, String actionID, IAction action) {
		fGlobalActions.put(actionID, action); 
		actionBars.setGlobalActionHandler(actionID, action);
	}
	
	/**
	 * Configures the toolBar.
	 * 
	 * @param tbm The toolbar that will be configured
	 */
	protected void configureToolBar(IToolBarManager tbm) {
		tbm.add(new Separator(this.getClass().getName()));
		tbm.add(getShowTypesAction());
		tbm.add(getShowQualifiedAction());
		tbm.add(new Separator(DebugUIMessages.getString("VariablesView.TOGGLE_VIEW_1"))); //$NON-NLS-1$
		tbm.add(getShowDetailPaneAction());
	}

   /**
	* Adds items to the tree viewer's context menu including any extension defined
	* actions.
	* 
	* @param menu The menu to add the item to.
	*/
	protected void fillContextMenu(IMenuManager menu) {

		menu.add(new Separator(IDebugUIConstants.EMPTY_VARIABLE_GROUP));
		menu.add(new Separator(IDebugUIConstants.VARIABLE_GROUP));
		menu.add(getAddToInspectorAction());
		menu.add(getChangeVariableAction());
		menu.add(getCopyToClipboardAction());
		menu.add(new Separator(IDebugUIConstants.EMPTY_RENDER_GROUP));
		menu.add(new Separator(IDebugUIConstants.RENDER_GROUP));
		menu.add(getShowTypesAction());
		menu.add(getShowQualifiedAction());
		
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
   /**
	* Adds items to the detail area's context menu including any extension defined
	* actions.
	* 
	* @param menu The menu to add the item to.
	*/
	protected void fillDetailContextMenu(IMenuManager menu) {
		menu.add((IAction)fGlobalActions.get(ITextEditorActionConstants.COPY));

		menu.add(new Separator("FIND")); //$NON-NLS-1$
		menu.add((IAction)fGlobalActions.get(ITextEditorActionConstants.FIND));
		
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	/**
	 * Lazily instantiate and return a selection listener that populates the detail pane,
	 * but only if the detail is currently visible. 
	 */
	protected ISelectionChangedListener getTreeSelectionChangedListener() {
		if (fTreeSelectionChangedListener == null) {
			fTreeSelectionChangedListener = new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					// if the detail pane is not visible, don't waste time retrieving details
					if (getSashForm().getMaximizedControl() == getViewer().getControl()) {
						return;
					}					
					IStructuredSelection selection = (IStructuredSelection)event.getSelection();
					populateDetailPaneFromSelection(selection);
				}					
			};
		}
		return fTreeSelectionChangedListener;
	}
	
	/**
	 * Show the details associated with the first of the selected variables in the 
	 * detail pane.
	 */
	protected void populateDetailPaneFromSelection(IStructuredSelection selection) {
		try {
			if (!selection.isEmpty()) {
				IVariable var = (IVariable)selection.getFirstElement();
				IValue val = var.getValue();
				String detail = getModelPresentation().getDetail(val);
				getDetailDocument().set(detail);
			} else {
				getDetailDocument().set(""); //$NON-NLS-1$
			}
		} catch (DebugException de) {
			DebugUIPlugin.logError(de);
		}				
	}
	
	/**
	 * Lazily instantiate and return a selection listener that updates the enabled
	 * state of the selection oriented actions in this view.
	 */
	protected ISelectionChangedListener getDetailSelectionChangedListener() {
		if (fDetailSelectionChangedListener == null) {
			fDetailSelectionChangedListener = new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					updateSelectionDependentActions();					
				}
			};
		}
		return fDetailSelectionChangedListener;
	}
	
	/**
	 * Lazily instantiate and return a document listener that updates the enabled state
	 * of the 'Find/Replace' action.
	 */
	protected IDocumentListener getDetailDocumentListener() {
		if (fDetailDocumentListener == null) {
			fDetailDocumentListener = new IDocumentListener() {
				public void documentAboutToBeChanged(DocumentEvent event) {
				}
				public void documentChanged(DocumentEvent event) {
					updateAction(ITextEditorActionConstants.FIND);
				}
			};
		}
		return fDetailDocumentListener;
	}
	
	/**
	 * Lazily instantiate and return a Document for the detail pane text viewer.
	 */
	protected IDocument getDetailDocument() {
		if (fDetailDocument == null) {
			fDetailDocument = new Document();
		}
		return fDetailDocument;
	}
	
	protected IDebugModelPresentation getModelPresentation() {
		if (fModelPresentation == null) {
			fModelPresentation = new DelegatingModelPresentation();
		}
		return fModelPresentation;
	}
	
	protected ITextViewer getDetailTextViewer() {
		return fDetailTextViewer;
	}
	
	protected SashForm getSashForm() {
		return fSashForm;
	}
	
	/**
	 * @see WorkbenchPart#getAdapter(Class)
	 */
	public Object getAdapter(Class required) {
		if (IFindReplaceTarget.class.equals(required)) {
			return getDetailTextViewer().getFindReplaceTarget();
		}
		return super.getAdapter(required);
	}

	protected void updateSelectionDependentActions() {
		Iterator iterator= fSelectionActions.iterator();
		while (iterator.hasNext())
			updateAction((String)iterator.next());		
	}

	protected void updateAction(String actionId) {
		IAction action= (IAction)fGlobalActions.get(actionId);
		if (action instanceof IUpdate)
			((IUpdate) action).update();
	}
	
	/**
	 * @see IDoubleClickListener#doubleClick(DoubleClickEvent)
	 */
	public void doubleClick(DoubleClickEvent event) {
		if (getChangeVariableAction().isEnabled()) {
			getChangeVariableAction().run();
		}
	}
	
	protected AddToInspectorAction getAddToInspectorAction() {
		return fAddToInspectorAction;
	}

	protected void setAddToInspectorAction(AddToInspectorAction addToInspectorAction) {
		fAddToInspectorAction = addToInspectorAction;
	}

	protected ChangeVariableValueAction getChangeVariableAction() {
		return fChangeVariableAction;
	}

	protected void setChangeVariableAction(ChangeVariableValueAction changeVariableAction) {
		fChangeVariableAction = changeVariableAction;
	}

	protected ControlAction getCopyToClipboardAction() {
		return fCopyToClipboardAction;
	}

	protected void setCopyToClipboardAction(ControlAction copyToClipboardAction) {
		fCopyToClipboardAction = copyToClipboardAction;
	}

	protected ShowQualifiedAction getShowQualifiedAction() {
		return fShowQualifiedAction;
	}

	protected void setShowQualifiedAction(ShowQualifiedAction showQualifiedAction) {
		fShowQualifiedAction = showQualifiedAction;
	}

	protected ShowTypesAction getShowTypesAction() {
		return fShowTypesAction;
	}

	protected void setShowTypesAction(ShowTypesAction showTypesAction) {
		fShowTypesAction = showTypesAction;
	}
	
	protected void setShowDetailPaneAction(ShowVariableDetailPaneAction action) {
		fShowDetailPaneAction = action;
	}
	
	protected ShowVariableDetailPaneAction getShowDetailPaneAction() {
		return fShowDetailPaneAction;
	}
}

