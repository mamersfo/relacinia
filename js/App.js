import React, { Component } from 'react';

import {
  QueryRenderer,
  graphql
} from 'react-relay';

import environment from './createRelayEnvironment';
import TeamDetails from './TeamDetails';

class App extends Component {
  render() {
    return (
      <div className="App">
        <h2>Relacinia</h2>
        <QueryRenderer
           environment={environment}

           query={graphql`query AppTeamQuery { teams { ...TeamDetails } }`}

           render={({error, props}) => {
               if (error) {
									 return <div>{error.message}</div>;
               } else if (props) {
									 return (
											 <ol>
												 {props.teams.map((team,i) => <TeamDetails key={i} data={team} />)}
											 </ol>					   
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
