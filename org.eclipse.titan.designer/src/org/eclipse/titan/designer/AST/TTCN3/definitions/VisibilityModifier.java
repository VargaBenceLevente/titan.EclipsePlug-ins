/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.definitions;

/**
 * The visibility modifier describing how TTCN-3 definitions importations and
 * groups are visible from other modules.
 *
 * @author Kristof Szabados
 * */
public enum VisibilityModifier {
	Friend, Public, Private
}
