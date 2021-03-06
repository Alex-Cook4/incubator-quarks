/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package quarks.runtime.etiao;

import java.util.List;

import quarks.execution.services.RuntimeServices;
import quarks.function.Consumer;
import quarks.oplet.JobContext;

/**
 * Context information for the {@code Oplet}'s execution context.
 *
 * @param <I>
 *            Data container type for input tuples.
 * @param <O>
 *            Data container type for output tuples.
 */
public class InvocationContext<I, O> extends AbstractContext<I, O> {

	private final String id;
	private final int inputCount;

	/**
	 * Creates an {@code InvocationContext} with the specified parameters.
	 *  
	 * @param id the oplet's unique identifier
	 * @param job the current job's context
	 * @param services service provider for the current job
	 * @param inputCount number of oplet's inputs 
	 * @param outputs list of oplet's outputs
	 */
    public InvocationContext(String id, JobContext job,
            RuntimeServices services,
            int inputCount,
            List<? extends Consumer<O>> outputs) {
        super(job, services);
        this.id = id;
        this.inputCount = inputCount;
        this.outputs = outputs;
    }

    private final List<? extends Consumer<O>> outputs;
    
    @Override
    public String getId() {
    	return id;
    }

    @Override
    public List<? extends Consumer<O>> getOutputs() {
        return outputs;
    }
    @Override
    public int getInputCount() {
        return inputCount;
    }
    @Override
    public int getOutputCount() {
        return getOutputs().size();
    }
}
