package com.meinc.ergo.exception;

public class RoleNotFoundException 
extends Exception
{
    private static final long serialVersionUID = 1L;
    
    public RoleNotFoundException()
    {
        super();
    }
    
    public RoleNotFoundException(String msg)
    {
        super(msg);
    }

}
