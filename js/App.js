import React, { Component } from 'react';

import {
  QueryRenderer,
  graphql
} from 'react-relay';

import environment from './createRelayEnvironment';
import TeamList from './TeamList';
import PlayerOptions from './PlayerOptions';
import TeamOptions from './TeamOptions';

class App extends Component {

		render() {
				return (
						<div className="App">
							<h2>Relacinia</h2>
							<QueryRenderer
								 environment={environment}
								 
								 query={graphql`query AppQuery { 
                   teamList: teams { ...TeamList } 
			      		   allPlayers: players { ...PlayerOptions } 
					         allTeams: teams { ...TeamOptions } 
								 }`}
								 
								 render={({error, props}) => {
										 if (error) {
												 return <div>{error.message}</div>;
										 } else if (props) {
									 return (
											 <div>
												 <TeamList data={props.teamList} />
												 <form>
 													 <PlayerOptions data={props.allPlayers}/>
													 <TeamOptions data={props.allTeams}/>
												 </form>
											 </div>
									 );
										 } else {
												 return <div>Loading</div>;
										 }
								 }}
								/>
								</div>
				);
		}
}

export default App;
