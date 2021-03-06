/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package leap.orm.mapping.config;

import leap.core.annotation.Inject;
import leap.orm.mapping.*;
import leap.orm.metadata.MetadataException;
import leap.orm.sharding.ShardingFactory;

public class ConfigMapper implements Mapper {

    protected @Inject MappingConfigSource source;
    protected @Inject ShardingFactory     shardingFactory;

    private final GlobalFieldMapper globalFieldMapper = new GlobalFieldMapper();
    private final ShardingMapper    shardingMapper    = new ShardingMapper();

    @Override
    public void completeMappings(MappingConfigContext context) throws MetadataException {
        //load mapping configs.
        MappingConfig config = source.load(context.getOrmContext());

        //process the loaded mapping configs.
        processLoadedMappingConfigs(context, config);
    }

    protected void processLoadedMappingConfigs(MappingConfigContext context, MappingConfig config) {

        globalFieldMapper.processGlobalFields(context, config);

        shardingMapper.processShardings(shardingFactory, context, config);
    }

}