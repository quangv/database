/**

Copyright (C) SYSTAP, LLC 2006-2011.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package com.bigdata.rdf.internal.constraints;

import java.util.Map;

import com.bigdata.bop.BOp;
import com.bigdata.bop.BOpBase;
import com.bigdata.bop.Constant;
import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.IConstant;
import com.bigdata.bop.IValueExpression;
import com.bigdata.bop.IVariable;
import com.bigdata.bop.IVariableOrConstant;
import com.bigdata.bop.ImmutableBOp;
import com.bigdata.bop.NV;
import com.bigdata.rdf.error.SparqlTypeErrorException;
import com.bigdata.rdf.internal.IV;
import com.bigdata.rdf.internal.Range;

/**
 * Operator used to impose a key-range constraint on an access path.
 * 
 * @author mrpersonick
 */
final public class RangeBOp extends BOpBase 
		implements IVariable<Range> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 3368581489737593349L;
	
//	private static final Logger log = Logger.getLogger(RangeBOp.class);
	
	public interface Annotations extends ImmutableBOp.Annotations {

		String VAR = (RangeBOp.class.getName() + ".var").intern();

		/** The inclusive lower bound. */
		String FROM = (RangeBOp.class.getName() + ".from").intern();
		
		/** The exclusive upper bound. */
		String TO = (RangeBOp.class.getName() + ".to").intern();
		
    }
    
	@SuppressWarnings("rawtypes")
    public RangeBOp(final IVariable<IV> var,
    		final IValueExpression<IV> from, 
    		final IValueExpression<IV> to) {

        this(NOARGS, 
        		NV.asMap(new NV(Annotations.VAR, var),
        				 new NV(Annotations.FROM, from),
        				 new NV(Annotations.TO, to)));

    }

	/**
	 * Required shallow copy constructor.
	 */
    public RangeBOp(final BOp[] args, final Map<String,Object> anns) {
    
        super(args,anns);

		if (getProperty(Annotations.VAR) == null
				|| getProperty(Annotations.FROM) == null
				|| getProperty(Annotations.TO) == null) {

			throw new IllegalArgumentException();
		
		}

    }

    /**
     * Required deep copy constructor.
     */
    public RangeBOp(final RangeBOp op) {

        super(op);
        
    }

    public IVariable<IV> var() {
    	return (IVariable<IV>) getRequiredProperty(Annotations.VAR);
    }
    
	public IValueExpression<IV> from() {
		if (from == null) {
			from = (IValueExpression<IV>) getRequiredProperty(Annotations.FROM);
		}
		return from;
	}
    
	public IValueExpression<IV> to() {
		if (to == null) {
			to = (IValueExpression<IV>) getRequiredProperty(Annotations.TO);
		}
		return to;
    }

	// cache to/from lookups.
    private transient volatile IValueExpression<IV> to, from;
    
    final public Range get(final IBindingSet bs) {
        
//    	log.debug("getting the asBound value");
    	
    	final IV from = from().get(bs);
    	
//    	log.debug("from: " + from);
    	
    	// sort of like Var.get(), which returns null when the variable
    	// is not yet bound
		if (from == null)
			return null;
    	
    	final IV to = to().get(bs);
    	
//    	log.debug("to: " + to);
    	
    	// sort of like Var.get(), which returns null when the variable
    	// is not yet bound
		if (to == null)
			return null;

    	try {
    		// let Range ctor() do the type checks and valid range checks
    		return new Range(from, to);
    	} catch (IllegalArgumentException ex) {
    		// log the reason the range is invalid
//    		if (log.isInfoEnabled())
//    			log.info("dropping solution: " + ex.getMessage());
    		// drop the solution
    		throw new SparqlTypeErrorException();
    	}
    	
    }
    
    final public RangeBOp asBound(final IBindingSet bs) {

		IV from, to;
		try {
			// log.debug("getting the asBound value");

			from = from().get(bs);

			// log.debug("from: " + from);

			// sort of like Var.get(), which returns null when the variable
			// is not yet bound
			if (from == null)
				return this;

			to = to().get(bs);

			// log.debug("to: " + to);

			// sort of like Var.get(), which returns null when the variable
			// is not yet bound
			if (to == null)
				return this;

		} catch (SparqlTypeErrorException ex) {

			/*
			 * Ignore. If the variables in the RangeBOp value expressions are
			 * not fully bound or has the wrong dynamic type then the range bop
			 * can not be evaluated yet.
			 */
			
			return this;
	
		}

		// Note: defer clone() until everything is bound.
		final RangeBOp asBound = (RangeBOp) this.clone();

		asBound._setProperty(Annotations.FROM, new Constant(from));
		asBound._setProperty(Annotations.TO, new Constant(to));

		return asBound;

	}
    
    final public boolean isFullyBound() {
    	
    	return from() instanceof IConstant && to() instanceof IConstant;
    	
    }

//	@Override
	public boolean isVar() {
		return true;
	}

//	@Override
	public boolean isConstant() {
		return false;
	}

//	@Override
	public Range get() {
//		log.debug("somebody tried to get me");
		
		return null;
	}

//	@Override
	public String getName() {
		return var().getName();
	}

//	@Override
	public boolean isWildcard() {
		return false;
	}


    final public boolean equals(final IVariableOrConstant op) {

    	if (op == null)
    		return false;
    	
    	if (this == op) 
    		return true;

        if (op instanceof IVariable<?>) {

            return var().getName().equals(((IVariable<?>) op).getName());

        }
        
        return false;
    	
    }
    
//    final private boolean _equals(final RangeBOp op) {
//    	
//    	return var().equals(op.var())
//    		&& from().equals(op.from())
//    		&& to().equals(op.to());
//
//    }
    
	/**
	 * Caches the hash code.
	 */
//	private int hash = 0;
	public int hashCode() {
//		
//		int h = hash;
//		if (h == 0) {
//			h = 31 * h + var().hashCode();
//			h = 31 * h + from().hashCode();
//			h = 31 * h + to().hashCode();
//			hash = h;
//		}
//		return h;
//
		return var().hashCode();
	}

}
