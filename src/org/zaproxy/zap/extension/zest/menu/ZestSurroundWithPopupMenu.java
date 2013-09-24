/**
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * @author Alessandro Secco: seccoale@gmail.com
 */
package org.zaproxy.zap.extension.zest.menu;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mozilla.zest.core.v1.ZestConditional;
import org.mozilla.zest.core.v1.ZestElement;
import org.mozilla.zest.core.v1.ZestExpression;
import org.mozilla.zest.core.v1.ZestExpressionAnd;
import org.mozilla.zest.core.v1.ZestExpressionEquals;
import org.mozilla.zest.core.v1.ZestExpressionOr;
import org.mozilla.zest.core.v1.ZestExpressionRegex;
import org.mozilla.zest.core.v1.ZestExpressionResponseTime;
import org.mozilla.zest.core.v1.ZestExpressionStatusCode;
import org.mozilla.zest.core.v1.ZestExpressionURL;
import org.mozilla.zest.core.v1.ZestLoop;
import org.mozilla.zest.core.v1.ZestLoopFile;
import org.mozilla.zest.core.v1.ZestLoopInteger;
import org.mozilla.zest.core.v1.ZestLoopString;
import org.mozilla.zest.core.v1.ZestScript;
import org.mozilla.zest.core.v1.ZestStatement;
import org.mozilla.zest.core.v1.ZestStructuredExpression;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.ExtensionPopupMenuItem;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.script.ScriptNode;
import org.zaproxy.zap.extension.zest.ExtensionZest;
import org.zaproxy.zap.extension.zest.ZestZapUtils;

public class ZestSurroundWithPopupMenu extends ExtensionPopupMenuItem {
	private static final long serialVersionUID = -5847208243296422433L;
	private ExtensionZest extension;
	private List<ExtensionPopupMenuItem> subMenus = new ArrayList<>();

	/**
	 * This method initializes
	 * 
	 */
	public ZestSurroundWithPopupMenu(ExtensionZest extension) {
		super("SurroundWithX");
		this.extension = extension;
	}

	/**/
	@Override
	public String getParentMenuName() {
		return Constant.messages.getString("zest.surround.with.popup");
	}

	@Override
	public boolean isSubMenu() {
		return true;
	}

	@Override
	public boolean isDummyItem() {
		return true;
	}

	public boolean isEnableForComponent(Component invoker) {
		for (ExtensionPopupMenuItem menu : subMenus) {
			View.getSingleton().getPopupMenu().removeMenu(menu);
		}
		subMenus.clear();
		// Remove previous submenus
		if (extension.isScriptTree(invoker)) {
			List<ScriptNode> selectedNodes = extension.getSelectedZestNodes();
			if (selectedNodes == null || selectedNodes.isEmpty()) {
				return false;
			}
			boolean containsExpression=false;
			boolean containsStatement = false;
			for(ScriptNode node:selectedNodes){
				if(ZestZapUtils.getElement(node) instanceof ZestExpression){
					containsExpression=true;
					if(containsStatement){
						return false;
					}
				} else if (ZestZapUtils.getElement(node) instanceof ZestStatement){
					containsStatement=true;
					if(containsExpression){
						return false;
					}
				}
			}
			ScriptNode parent = selectedNodes.get(0).getParent();
			if (parent == null) {
				return false;
			} else if (extension.getSelectedZestElements().get(0) instanceof ZestScript) {
				return false;
			} else if (ZestZapUtils.getShadowLevel(extension.getSelectedZestNodes().get(0))>0){
				return false;
			} else if (extension.getSelectedZestElements().get(0) instanceof ZestExpression) {
				reCreateSubMenu(parent, selectedNodes);
				return true;
			}/*
			 * else if (extension.getSelectedZestElements().get(0) instanceof
			 * ZestConditional && ZestZapUtils.getShadowLevel(extension
			 * .getAllZestScriptNodes().get(0)) == 0) { return false; }
			 */
			// ZestElement parentElem = ZestZapUtils.getElement(extension
			// .getSelectedZestNode());
			// if (parentElem instanceof ZestExpression) {
			// return false;
			// }
			reCreateSubMenu(parent, selectedNodes);
			return true;
		}
		return false;
	}

	private void reCreateSubMenu(ScriptNode parent, List<ScriptNode> children) {
		ZestElement firstElemSelected = ZestZapUtils
				.getElement(children.get(0));
		if (firstElemSelected instanceof ZestExpression) {
			createPopupAddActionMenu(parent, children, new ZestExpressionOr());
			createPopupAddActionMenu(parent, children, new ZestExpressionAnd());
		} else if (firstElemSelected instanceof ZestStatement) {
			createPopupAddActionMenu(parent, children, new ZestLoopString());
			try {
				createPopupAddActionMenu(parent, children, new ZestLoopFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
			createPopupAddActionMenu(parent, children, new ZestLoopInteger());
			createPopupAddActionMenu(parent, children, new ZestConditional(
					new ZestExpressionOr()));
			createPopupAddActionMenu(parent, children, new ZestConditional(
					new ZestExpressionAnd()));
			createPopupAddActionMenu(parent, children, new ZestConditional(
					new ZestExpressionEquals()));
			createPopupAddActionMenu(parent, children, new ZestConditional(
					new ZestExpressionRegex()));
			createPopupAddActionMenu(parent, children, new ZestConditional(
					new ZestExpressionResponseTime()));
			createPopupAddActionMenu(parent, children, new ZestConditional(
					new ZestExpressionStatusCode()));
			createPopupAddActionMenu(parent, children, new ZestConditional(
					new ZestExpressionURL()));

		}
	}

	private void createPopupAddActionMenu(final ScriptNode parent,
			final List<ScriptNode> children, final ZestElement za) {
		ZestPopupMenu menu;
		if (za instanceof ZestExpressionAnd) {
			menu = new ZestPopupMenu(
					Constant.messages.getString("zest.surround.with.popup"),
					Constant.messages
							.getString("zest.condition.add.popup.empty.and"));
		} else if (za instanceof ZestExpressionOr) {
			menu = new ZestPopupMenu(
					Constant.messages.getString("zest.surround.with.popup"),
					Constant.messages
							.getString("zest.condition.add.popup.empty.or"));
		} else {
			menu = new ZestPopupMenu(
					Constant.messages.getString("zest.surround.with.popup"),
					ZestZapUtils.toUiString(za, false));
		}
		if (za instanceof ZestLoop<?>) {
			menu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					extension.getDialogManager().showZestLoopDialog(parent,
							children, null, (ZestLoop<?>) za, true, true);
				}
			});
		} else if (za instanceof ZestConditional) {
			final ZestExpression expr = (ZestExpression) ((ZestConditional) za)
					.getRootExpression();
			menu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (expr instanceof ZestStructuredExpression) {
						ZestConditional zc=(ZestConditional)za;
						for(ScriptNode node:children){
							extension.delete(node);
							ZestStatement ifStmt=(ZestStatement)ZestZapUtils.getElement(node);
							zc.addIf(ifStmt);
						}
						extension.addToParent(parent,zc);
//						extension.setCnpNodes(children);
//						extension.setCut(true);
//						extension.pasteToNode(condNode);
					} else {
						extension.getDialogManager().showZestExpressionDialog(
								parent, children, null, expr, true, true, true);
					}
				}
			});
		} else if (za instanceof ZestExpressionAnd) {
			menu.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ScriptNode andNode = extension.addToParent(parent,
							(ZestExpressionAnd) za);
					extension.setCnpNodes(children);
					 extension.setCut(true);
					extension.pasteToNode(andNode);
				}
			});
		} else if (za instanceof ZestExpressionOr) {
			menu.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ScriptNode orNode = extension.addToParent(parent,
							(ZestExpressionOr) za);
					extension.setCnpNodes(children);
					extension.setCut(true);
					extension.pasteToNode(orNode);
				}
			});
		}
		menu.setMenuIndex(this.getMenuIndex());
		View.getSingleton().getPopupMenu().addMenu(menu);
		this.subMenus.add(menu);
	}

	@Override
	public boolean isSafe() {
		return true;
	}

}