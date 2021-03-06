/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values.expressions;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Altstep;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Extfunction;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Function;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Testcase;
import org.eclipse.titan.designer.AST.TTCN3.types.Altstep_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.Function_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.Testcase_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.Altstep_Reference_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Function_Reference_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Testcase_Reference_Value;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents a refers expression.
 * <p>
 * In the compiler this is a direct value.
 *
 * @author Kristof Szabados
 * */
public final class RefersExpression extends Expression_Value {
	private static final String OPERANDERROR = "Reference to a function, external function, altstep or testcase was expected.";

	private final Reference referred;
	private Assignment referredAssignment;

	public RefersExpression(final Reference referred) {
		this.referred = referred;

		if (referred != null) {
			referred.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Operation_type getOperationType() {
		return Operation_type.REFERS_OPERATION;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReference(final CompilationTimeStamp timestamp, final Assignment lhs) {
		if (lhs == referredAssignment) {
			return true;
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		if (referred == null) {
			return "<erroneous value>";
		}

		final StringBuilder builder = new StringBuilder();
		builder.append("refers(").append(referred.getDisplayName()).append(')');
		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (referred != null) {
			referred.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (referred == child) {
			return builder.append(OPERAND);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		if (referredAssignment == null) {
			evaluateValue(timestamp, expectedValue, null);
		}

		if(referredAssignment == null) {
			return Type_type.TYPE_UNDEFINED;
		}

		switch (referredAssignment.getAssignmentType()) {
		case A_FUNCTION:
		case A_FUNCTION_RTEMP:
		case A_FUNCTION_RVAL:
		case A_EXT_FUNCTION:
		case A_EXT_FUNCTION_RTEMP:
		case A_EXT_FUNCTION_RVAL:
			return Type_type.TYPE_FUNCTION;
		case A_ALTSTEP:
			return Type_type.TYPE_ALTSTEP;
		case A_TESTCASE:
			return Type_type.TYPE_TESTCASE;
		default:
			return Type_type.TYPE_UNDEFINED;
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		return false;
	}

	/**
	 * Checks the parameters of the expression and if they are valid in
	 * their position in the expression or not.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param expectedValue
	 *                the kind of value expected.
	 * @param referenceChain
	 *                a reference chain to detect cyclic references.
	 * */
	private void checkExpressionOperands(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (referred == null) {
			return;
		}

		referredAssignment = referred.getRefdAssignment(timestamp, false);
		if (referredAssignment == null) {
			return;
		}

		switch (referredAssignment.getAssignmentType()) {
		case A_FUNCTION:
		case A_FUNCTION_RTEMP:
		case A_FUNCTION_RVAL:
		case A_EXT_FUNCTION:
		case A_EXT_FUNCTION_RTEMP:
		case A_EXT_FUNCTION_RVAL:
		case A_ALTSTEP:
		case A_TESTCASE:
			break;
		default:
			location.reportSemanticError(OPERANDERROR);
			setIsErroneous(true);
			break;
		}
	}

	@Override
	/** {@inheritDoc} */
	public IValue evaluateValue(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return lastValue;
		}

		isErroneous = false;
		lastTimeChecked = timestamp;
		lastValue = this;

		if (referred == null) {
			return lastValue;
		}

		checkExpressionOperands(timestamp, expectedValue, referenceChain);

		if (getIsErroneous(timestamp) || referredAssignment == null) {
			return lastValue;
		}

		if (isUnfoldable(timestamp, referenceChain)) {
			return lastValue;
		}

		switch (referredAssignment.getAssignmentType()) {
		case A_FUNCTION:
		case A_FUNCTION_RTEMP:
		case A_FUNCTION_RVAL:
			lastValue = new Function_Reference_Value((Def_Function) referredAssignment);
			lastValue.copyGeneralProperties(this);
			break;
		case A_EXT_FUNCTION:
		case A_EXT_FUNCTION_RTEMP:
		case A_EXT_FUNCTION_RVAL:
			lastValue = new Function_Reference_Value((Def_Extfunction) referredAssignment);
			lastValue.copyGeneralProperties(this);
			break;
		case A_ALTSTEP:
			lastValue = new Altstep_Reference_Value((Def_Altstep) referredAssignment);
			lastValue.copyGeneralProperties(this);
			break;
		case A_TESTCASE:
			lastValue = new Testcase_Reference_Value((Def_Testcase) referredAssignment);
			lastValue.copyGeneralProperties(this);
			break;
		default:
			setIsErroneous(true);
			break;
		}
		// transform

		return lastValue;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (referred != null) {
			referred.updateSyntax(reparser, false);
			reparser.updateLocation(referred.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (referred == null) {
			return;
		}

		referred.findReferences(referenceFinder, foundIdentifiers);
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (referred != null && !referred.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpressionExpression(final JavaGenData aData, final ExpressionStruct expression) {
		IType governor = myGovernor;
		if (governor == null) {
			governor = getExpressionGovernor(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE);
		}
		if (governor == null) {
			governor = myLastSetGovernor;
		}
		if (governor == null || referredAssignment == null) {
			ErrorReporter.INTERNAL_ERROR("FATAL ERROR while generating code for expression `" + getFullName() + "''");
			return;
		}

		final IType lastGovernor = governor.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());
		final String moduleName = referredAssignment.getMyScope().getModuleScopeGen().getName();
		final String functionName = referredAssignment.getIdentifier().getName();

		expression.expression.append(MessageFormat.format("new {0}(new {0}.function_pointer() '{'\n", governor.getGenNameValue(aData, expression.expression)));
		expression.expression.append("@Override\n");
		expression.expression.append("public String getModuleName() {\n");
		expression.expression.append(MessageFormat.format("return \"{0}\";\n", moduleName));
		expression.expression.append("}\n");
		expression.expression.append("@Override\n");
		expression.expression.append("public String getDefinitionName() {\n");
		expression.expression.append(MessageFormat.format("return \"{0}\";\n", functionName));
		expression.expression.append("}\n");
		if (lastGovernor.getTypetype().equals(Type_type.TYPE_FUNCTION)) {
			expression.expression.append("@Override\n");
			expression.expression.append("public ");
			final Function_Type functionType = (Function_Type) lastGovernor;
			final Type returnType = functionType.getReturnType();
			final StringBuilder actualParList = functionType.getFormalParameters().generateCodeActualParlist("");

			if (returnType == null) {
				expression.expression.append("void");
			} else {
				if (functionType.returnsTemplate()) {
					expression.expression.append(returnType.getGenNameTemplate(aData, expression.expression));
				} else {
					expression.expression.append(returnType.getGenNameValue(aData, expression.expression));
				}
			}
			expression.expression.append(" invoke(");
			functionType.getFormalParameters().generateCode(aData, expression.expression);
			expression.expression.append(") {\n");
			if (returnType != null) {
				expression.expression.append("return ");
			}
			expression.expression.append(referredAssignment.getGenNameFromScope(aData, expression.expression, ""));
			expression.expression.append('(');
			expression.expression.append(actualParList);
			expression.expression.append(");\n");
			expression.expression.append("}\n");

			if (functionType.isStartable(CompilationTimeStamp.getBaseTimestamp())) {
				aData.addBuiltinTypeImport("TitanComponent");

				expression.expression.append("@Override\n");
				expression.expression.append("public void start(final TitanComponent component_reference");
				if (functionType.getFormalParameters().getNofParameters() > 0) {
					expression.expression.append(", ");
					functionType.getFormalParameters().generateCode(aData, expression.expression);
				}
				expression.expression.append(") {\n");
				expression.expression.append(MessageFormat.format("{0}.start_{1}(component_reference", moduleName, functionName));
				if (actualParList != null && actualParList.length() > 0) {
					expression.expression.append(MessageFormat.format(", {0}", actualParList));
				}
				expression.expression.append(");\n");
				expression.expression.append("}\n");
			}

		} else if (lastGovernor.getTypetype().equals(Type_type.TYPE_ALTSTEP)) {
			aData.addBuiltinTypeImport("Default_Base");
			aData.addBuiltinTypeImport("TitanAlt_Status");

			final Altstep_Type altstepType = (Altstep_Type) lastGovernor;
			final String altstepName = referredAssignment.getIdentifier().getName();
			final String altstepGenName = referredAssignment.getGenNameFromScope(aData, expression.expression, "");
			final StringBuilder actualParList = altstepType.getFormalParameters().generateCodeActualParlist("");

			expression.expression.append("@Override\n");
			expression.expression.append("public void invoke_standalone(");
			altstepType.getFormalParameters().generateCode(aData, expression.expression);
			expression.expression.append(") {\n");
			expression.expression.append(MessageFormat.format("{0}({1});\n", altstepGenName, actualParList));
			expression.expression.append("}\n");

			expression.expression.append("@Override\n");
			expression.expression.append("public Default_Base activate(");
			altstepType.getFormalParameters().generateCode(aData, expression.expression);
			expression.expression.append(") {\n");
			expression.expression.append(MessageFormat.format("return activate_{0}({1});\n", altstepName, actualParList));
			expression.expression.append("}\n");

			expression.expression.append("@Override\n");
			expression.expression.append("public TitanAlt_Status invoke(");
			altstepType.getFormalParameters().generateCode(aData, expression.expression);
			expression.expression.append(") {\n");
			expression.expression.append(MessageFormat.format("return {0}_instance({1});\n", altstepName, actualParList));
			expression.expression.append("}\n");
		} else if (lastGovernor.getTypetype().equals(Type_type.TYPE_TESTCASE)) {
			aData.addBuiltinTypeImport("TitanVerdictType");
			aData.addBuiltinTypeImport("TitanFloat");

			expression.expression.append("@Override\n");
			expression.expression.append("public ");
			final Testcase_Type testcaseType = (Testcase_Type) lastGovernor;

			expression.expression.append("TitanVerdictType");
			expression.expression.append(" execute(");
			if(testcaseType.getFormalParameters().getNofParameters() > 0) {
				testcaseType.getFormalParameters().generateCode(aData, expression.expression);
				expression.expression.append(", ");
			}
			expression.expression.append("final boolean has_timer, final TitanFloat timer_value");
			expression.expression.append(") {\n");
			expression.expression.append("return testcase_");
			expression.expression.append(referredAssignment.getIdentifier().getName());
			expression.expression.append('(');
			if(testcaseType.getFormalParameters().getNofParameters() > 0) {
				expression.expression.append(testcaseType.getFormalParameters().generateCodeActualParlist(""));
				expression.expression.append(", ");
			}
			expression.expression.append("has_timer, timer_value");
			expression.expression.append(");\n");
			expression.expression.append("}\n");
		}
		expression.expression.append("})\n");
	}
}
