package com.papairs.docs.config;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class NanoIDGenerator implements IdentifierGenerator {
    
    private static final int DEFAULT_ID_LENGTH = 12;

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return NanoIdUtils.randomNanoId(
                NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
                NanoIdUtils.DEFAULT_ALPHABET,
                DEFAULT_ID_LENGTH
        );
    }
}
